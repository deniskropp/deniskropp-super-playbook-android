package com.example.domain

import kotlin.math.roundToInt

data class ValidationOutcome(
    val violations: List<String>,
    val undeclaredTokens: List<String>,
    val schemaPurity: Double,
    val detectedLogicTypes: Set<String>
)

object OcsRuleValidator {

    private val sectionTokenRegex = Regex("""⫻[a-zA-Z0-9_:/\-]+""")
    private val execTargetRegex = Regex("""⫻cmd/exec:\s*([^\n]+)""")

    fun validateObjectiveInput(objective: String): List<String> {
        val warnings = mutableListOf<String>()
        if (objective.contains(ProtocolRegistry.DEPRECATED_ALIAS, ignoreCase = true)) {
            warnings += "RULE-01 Violation: '${ProtocolRegistry.DEPRECATED_ALIAS}' is a deprecated alias and cannot be a ⫻cmd/exec target. Use 'Orchestrator'."
        }
        return warnings
    }

    fun validateStepOutput(
        step: MetaStepBlueprint,
        kickLangOutput: String,
        displayExecutable: String,
        displayInput: String,
        displayExpectedOutput: String
    ): ValidationOutcome {
        val violations = mutableListOf<String>()
        val undeclaredTokens = mutableListOf<String>()
        val detectedLogic = mutableSetOf<String>()

        // FORMAT-08 check
        if (displayExecutable.isBlank() || displayInput.isBlank() || displayExpectedOutput.isBlank()) {
            violations += "FORMAT-08: Missing Executable, Input, or Expected output in ⫻display/header."
        }

        // RULE-01 check (no deprecated alias; exec targets must be declared)
        if (kickLangOutput.contains(ProtocolRegistry.DEPRECATED_ALIAS, ignoreCase = true)) {
            violations += "RULE-01: Deprecated alias '${ProtocolRegistry.DEPRECATED_ALIAS}' detected in output."
        }
        val validTargets = ProtocolRegistry.atomicAgentNames + setOf("All", "KickForge", "KickFlow", "KickGuard")
        execTargetRegex.findAll(kickLangOutput).forEach { match ->
            val rawTargets = match.groupValues[1]
                .split(",", "&")
                .map { it.trim().substringBefore("→").substringBefore(" ").trim() }
                .filter { it.isNotBlank() }
            // Allow multi-word agent names cleanly
            val fullLine = match.groupValues[1].trim()
            val knownMatched = validTargets.any { fullLine.contains(it, ignoreCase = true) }
            if (!knownMatched && rawTargets.isNotEmpty()) {
                violations += "RULE-01: Unrecognized ⫻cmd/exec target in '$fullLine'."
            }
        }

        // RULE-02 & RULE-08: Every ⫻ section token must be declared
        val foundTokens = sectionTokenRegex.findAll(kickLangOutput).map { it.value }.toList()
        val hasPromptEnvelope = foundTokens.any { it.startsWith("⫻prompt/") }
        foundTokens.forEach { token ->
            val normalized = token.trimEnd(':')
            val declared = isDeclaredToken(normalized)
            if (!declared) {
                undeclaredTokens += normalized
            }
            if (ProtocolRegistry.promptFamilyTokens.any { normalized.startsWith(it) } && !hasPromptEnvelope) {
                violations += "RULE-08: Prompt-family token '$normalized' appeared outside a ⫻prompt/* envelope."
            }
            when {
                normalized.startsWith("⫻logic/if/error") -> {
                    detectedLogic += "if"
                    detectedLogic += "error"
                }
                normalized.startsWith("⫻logic/if") -> detectedLogic += "if"
                normalized.startsWith("⫻logic/else") -> detectedLogic += "else"
                normalized.startsWith("⫻logic/loop") -> detectedLogic += "loop"
                normalized.startsWith("⫻logic/error") -> detectedLogic += "error"
            }
        }

        val uniqueUndeclared = undeclaredTokens.distinct()
        if (uniqueUndeclared.isNotEmpty()) {
            violations += "RULE-02/RULE-08 Schema Drift: Undeclared tokens ${uniqueUndeclared.joinToString(", ")}."
        }

        // RULE-03: loop needs error; if needs else/error/halt
        val hasIf = "if" in detectedLogic
        val hasElse = "else" in detectedLogic
        val hasLoop = "loop" in detectedLogic
        val hasError = "error" in detectedLogic
        val hasHalt = kickLangOutput.contains("⫻cmd/halt")
        if (hasLoop && !hasError) {
            violations += "RULE-03: ⫻logic/loop declared without a ⫻logic/error handler."
        }
        if (hasIf && !(hasElse || hasError || hasHalt)) {
            violations += "RULE-03: ⫻logic/if declared without ⫻logic/else, ⫻logic/error, or ⫻cmd/halt."
        }

        // Schema purity ratio for Step 17 formula
        val totalTokens = foundTokens.size.coerceAtLeast(1)
        val cleanCount = (totalTokens - undeclaredTokens.size).coerceAtLeast(0)
        val purity = cleanCount.toDouble() / totalTokens.toDouble()

        return ValidationOutcome(
            violations = violations,
            undeclaredTokens = uniqueUndeclared,
            schemaPurity = round3(purity),
            detectedLogicTypes = detectedLogic + step.logicTypes
        )
    }

    fun isDeclaredToken(token: String): Boolean {
        if (token in ProtocolRegistry.declaredSectionPrefixes) return true
        return ProtocolRegistry.declaredSectionPrefixes.any { prefix ->
            prefix.endsWith("/") && token.startsWith(prefix)
        } || token.startsWith("⫻data/var:") ||
            token.startsWith("⫻data/cite:") ||
            token.startsWith("⫻data/spec:") ||
            token.startsWith("⫻data/rules:") ||
            token.startsWith("⫻data/visual:") ||
            token.startsWith("⫻data/ptas:") ||
            token.startsWith("⫻data/conflict:") ||
            token.startsWith("⫻plan/team:") ||
            token.startsWith("⫻plan/group:") ||
            token.startsWith("⫻context/klmx:")
    }

    /**
     * Exact Step 17 formula from Super Playbook v1.0.0:
     * structural_coherence = (agent_coverage / 14) * (step_completion / 20) * schema_purity
     * RULE-09: Never collapses sonic axis into structural score.
     */
    fun computeStructuralCoherence(
        invokedAtomicAgentCount: Int,
        stepsDone: Int,
        undeclaredDriftCount: Int
    ): Double {
        val agentRatio = (invokedAtomicAgentCount.coerceIn(0, 14) / 14.0)
        val stepRatio = (stepsDone.coerceIn(0, 20) / 20.0)
        val schemaPurity = (1.0 - (undeclaredDriftCount * 0.05)).coerceIn(0.5, 1.0)
        return round3(agentRatio * stepRatio * schemaPurity)
    }

    /**
     * RULE-10 Consent Gate condition for ⫻flow/qllickfizz_sonic:
     * Energy >= 4, adaptive tempo, or Path D full_bloom contrast.
     */
    fun requiresSonicConsentGate(
        energy: Int,
        adaptiveTempo: Boolean,
        contrastPath: SonicContrastPath
    ): Boolean = energy >= 4 || adaptiveTempo || contrastPath == SonicContrastPath.PATH_D_FULL_BLOOM

    fun validateFormat09Envelope(envelope: String): List<String> {
        val requiredOrdered = listOf(
            "⫻prompt/", "⫻role/", "⫻objective/", "⫻constraints/",
            "⫻task/", "⫻output-format/", "⫻evaluation/"
        )
        val missing = requiredOrdered.filter { !envelope.contains(it) }
        val violations = mutableListOf<String>()
        if (missing.isNotEmpty()) {
            violations += "FORMAT-09: Missing envelope sections ${missing.joinToString(", ")}."
        }
        val positions = requiredOrdered.mapNotNull { token ->
            val idx = envelope.indexOf(token)
            if (idx >= 0) token to idx else null
        }
        for (i in 1 until positions.size) {
            if (positions[i].second < positions[i - 1].second) {
                violations += "FORMAT-09: Section '${positions[i].first}' appears out of required order."
            }
        }
        return violations
    }

    private fun round3(value: Double): Double = (value * 1000.0).roundToInt() / 1000.0
}
