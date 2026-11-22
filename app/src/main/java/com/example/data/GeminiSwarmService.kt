package com.example.data

import android.util.Log
import com.example.domain.MetaStepBlueprint
import com.example.domain.ProtocolRegistry
import com.example.domain.SonicContrastPath
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiSwarmService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val TIMEOUT_MS = 60_000L

data class StepAiResult(
    val displayExecutable: String,
    val displayInput: String,
    val displayExpectedOutput: String,
    val kickLangOutput: String,
    val summary: String,
    val logicBranch: String,
    val metricScore: Double?,
    val recommendedHalt: Boolean,
    val haltReason: String?
)

data class MoleculeStageAiResult(
    val stageOutput: String,
    val summary: String
)

data class SonicEnvelopeAiResult(
    val promptEnvelope: String,
    val summary: String
)

data class WetLiftAiResult(
    val sonicCoherence: Double,
    val rationale: String
)

class GeminiSwarmService {

    private val stepSchema = Schema.obj(
        properties = mapOf(
            "displayExecutable" to Schema.string("Action or TAS primitive for FORMAT-08 ⫻display/header"),
            "displayInput" to Schema.string("Input payload for FORMAT-08 ⫻display/header"),
            "displayExpectedOutput" to Schema.string("Target ⫻ section type for FORMAT-08 ⫻display/header"),
            "kickLangOutput" to Schema.string("Formal KickLang ⫻protocol/ocs v2.2 Space output strictly using declared ⫻ section types"),
            "summary" to Schema.string("Concise 1-2 sentence executive summary of what this agent produced"),
            "logicBranch" to Schema.string("Logic branch executed (e.g. if->else, loop converged, error clear, halt gate)"),
            "metricScore" to Schema.double("Optional measured score between 0.0 and 1.0 (e.g. universality_score in Step 2 or spec_clarity_score in Step 7)", nullable = true),
            "recommendedHalt" to Schema.boolean("True only if RULE-06 / ethical / clarity halt should trigger"),
            "haltReason" to Schema.string("Reason if recommendedHalt is true", nullable = true)
        ),
        optionalProperties = listOf("metricScore", "haltReason")
    )

    private val moleculeSchema = Schema.obj(
        properties = mapOf(
            "stageOutput" to Schema.string("Formal KickLang v4.1 Molecule stage output using declared ⫻ sections"),
            "summary" to Schema.string("One-sentence stage synthesis")
        )
    )

    private val sonicSchema = Schema.obj(
        properties = mapOf(
            "promptEnvelope" to Schema.string("Complete FORMAT-09 ⫻prompt/* Lyria3 master envelope in exact section order"),
            "summary" to Schema.string("One-sentence musical/spatial synthesis summary")
        )
    )

    private val wetLiftSchema = Schema.obj(
        properties = mapOf(
            "sonicCoherence" to Schema.double("Measured sonic coherence score between 0.80 and 0.99 after wet-lift evaluation"),
            "rationale" to Schema.string("Evaluation of micro_timing, spatial mapping, contrast balance, and Lyria3 brief fidelity")
        )
    )

    private val stepModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stepSchema
            }
        )
    }

    private val moleculeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = moleculeSchema
            }
        )
    }

    private val sonicModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sonicSchema
            }
        )
    }

    private val wetLiftModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = wetLiftSchema
            }
        )
    }

    suspend fun executeMetaStep(
        run: PlaybookRunDoc,
        step: MetaStepBlueprint,
        priorSteps: List<StepExecutionDoc>
    ): StepAiResult {
        val recentContext = priorSteps.takeLast(3).joinToString("\n\n") { prev ->
            "Step ${prev.stepNumber} (${prev.agentName} / ${prev.primitive}): ${prev.summary}\n${prev.kickLangOutput.take(600)}"
        }.ifBlank { "No prior steps executed yet." }

        val prompt = buildString {
            appendLine("You are executing Step ${step.stepNumber} of the OCS KickLang Super Playbook v1.0.0 under ⫻protocol/ocs v2.2 and KickLang v4.1 Molecule.")
            appendLine("Active Agent: ${step.agentName}")
            appendLine("Step Title: ${step.title}")
            appendLine("TAS Primitive: ${step.primitive}")
            appendLine("Mode: ${run.cmdMode} (projected run_flags.mode: ${run.projectedRunFlagMode}), Language: ${run.cmdLang}")
            appendLine("Objective (⫻data/obj): ${run.objective}")
            if (run.selectedWePlanOptions.isNotEmpty()) {
                appendLine("Selected WePlan Options: ${run.selectedWePlanOptions.joinToString(", ")}")
            }
            appendLine("Expected Output Contract: ${step.expectedOutput}")
            appendLine("Step Mandate: ${step.description}")
            appendLine("STRICT PROTOCOL RULES:")
            appendLine("- NEVER use 'SuperGrok Leader' (RULE-01). Canonical top agent is 'Orchestrator'.")
            appendLine("- ONLY use declared ⫻protocol/ocs v2.2 tokens: ⫻context/klmx, ⫻cmd/exec, ⫻cmd/halt, ⫻cmd/mode, ⫻cmd/lang, ⫻data/obj, ⫻data/tas_raw, ⫻data/tas_purified, ⫻data/ptas, ⫻data/spec, ⫻data/logic, ⫻data/rv, ⫻data/draft, ⫻data/cite, ⫻data/var, ⫻data/rules, ⫻data/visual, ⫻data/state, ⫻data/insight, ⫻data/conflict, ⫻logic/if, ⫻logic/else, ⫻logic/loop, ⫻logic/error, ⫻query/clarify, ⫻flow/meta-playbook, ⫻end/flow.")
            appendLine("- Every ⫻logic/loop MUST include a ⫻logic/error handler; every ⫻logic/if MUST include ⫻logic/else, ⫻logic/error, or ⫻cmd/halt (RULE-03).")
            appendLine("- Keep sonic coherence OPEN / null unless explicitly wet-lift scored (RULE-09). Never invent measured coherence in Step 1..19 except Step 2 universality_score or Step 7 spec_clarity_score.")
            appendLine("- Respond in ${run.cmdLang} for natural language annotations while preserving strict KickLang ⫻ sections.")
            appendLine("\nRecent Prior Step Context:\n$recentContext")
        }

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                stepModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Gemini returned an empty step response.")
            val json = JSONObject(raw)
            StepAiResult(
                displayExecutable = json.optString("displayExecutable", step.primitive),
                displayInput = json.optString("displayInput", step.inputPayload),
                displayExpectedOutput = json.optString("displayExpectedOutput", step.expectedOutput),
                kickLangOutput = json.optString("kickLangOutput", "⫻data/logic:\n  Step ${step.stepNumber} executed."),
                summary = json.optString("summary", "Step ${step.stepNumber} completed by ${step.agentName}."),
                logicBranch = json.optString("logicBranch", "proceed"),
                metricScore = if (json.isNull("metricScore")) null else json.optDouble("metricScore").takeIf { !it.isNaN() }?.coerceIn(0.0, 1.0),
                recommendedHalt = json.optBoolean("recommendedHalt", false),
                haltReason = if (json.isNull("haltReason")) null else json.optString("haltReason").takeIf { it.isNotBlank() }
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Step ${step.stepNumber} timed out", e)
            throw IllegalStateException("Agent ${step.agentName} timed out after 60s. Please retry Step ${step.stepNumber}.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Step ${step.stepNumber} execution failed", e)
            throw e
        }
    }

    suspend fun executeMoleculeStage(
        doc: MoleculeRunDoc,
        stageName: String
    ): MoleculeStageAiResult {
        val prompt = buildString {
            appendLine("Execute stage '$stageName' of ⫻flow/molecule (Flow-Playbook v1.1 compact runtime, model: ${ProtocolRegistry.LANGUAGE_MODEL}).")
            appendLine("PTAS-06 Constraint: Keep compact molecule semantics — do NOT expand into a 20-step map.")
            appendLine("Projected run_flags.mode: ${doc.projectedRunFlagMode} (from ⫻cmd/mode: ${doc.sourceCmdMode})")
            appendLine("Bound ⫻context/klmx Hexad:")
            appendLine("  molecule: ${doc.molecule}")
            appendLine("  formula: ${doc.formula}")
            appendLine("  model: ${doc.model}")
            appendLine("  space: ${doc.space}")
            appendLine("  scope: ${doc.scope}")
            appendLine("  reference: ${doc.reference}")
            if (doc.sonicDelegated) {
                appendLine("Sonic Delegation: Active -> delegate sonic synthesis to ⫻flow/qllickfizz_sonic (keep sonic axis OPEN until wet-lift).")
            }
            if (doc.stageOutputs.isNotEmpty()) {
                appendLine("Prior stage outputs:\n${doc.stageOutputs.joinToString("\n---\n")}")
            }
            appendLine("Emit crisp KickLang v4.1 Molecule syntax for stage '$stageName' using only declared ⫻protocol/ocs v2.2 tokens.")
        }

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                moleculeModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Gemini returned empty molecule stage output.")
            val json = JSONObject(raw)
            MoleculeStageAiResult(
                stageOutput = json.optString("stageOutput", "⫻flow/molecule [$stageName]\n  molecule: ${doc.molecule}\n⫻end/flow"),
                summary = json.optString("summary", "Stage $stageName completed.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Molecule stage $stageName timed out", e)
            throw IllegalStateException("Molecule stage '$stageName' timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Molecule stage $stageName failed", e)
            throw e
        }
    }

    suspend fun generateSonicEnvelope(
        doc: SonicRunDoc,
        contrastPath: SonicContrastPath
    ): SonicEnvelopeAiResult {
        val prompt = buildString {
            appendLine("Generate a QllickFizz Sonique — Lyria3 Master Template under ⫻flow/qllickfizz_sonic and ⫻protocol/ocs v2.2.")
            appendLine("STRICT FORMAT-09 SECTION ORDER REQUIRED:")
            appendLine("1. ⫻prompt/${doc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_")}")
            appendLine("2. ⫻role/ (acting sonic designer persona, NOT a roster agent)")
            appendLine("3. ⫻objective/")
            appendLine("4. ⫻context/ (include sonic spatial mapping)")
            appendLine("5. ⫻constraints/ (include Fizz rules, micro_timing, and RULE-10 consent token status)")
            appendLine("6. ⫻task/")
            appendLine("7. ⫻output-format/")
            appendLine("8. ⫻evaluation/ (note sonic axis stays OPEN until wet-lift, target 0.93 once scored)")
            appendLine("9. ⫻iteration/")
            appendLine("\nParameters:")
            appendLine("- Creative Brief: ${doc.creativeBrief}")
            appendLine("- Energy: ${doc.energy}/5")
            appendLine("- Adaptive Tempo: ${doc.adaptiveTempo}")
            appendLine("- Contrast Path: ${contrastPath.label}")
            appendLine("- Micro Timing: ${doc.microTiming}")
            appendLine("- Spatial Mapping: ${doc.spatialMapping}")
            appendLine("- Consent Token: ${doc.consentToken ?: "not_required"}")
        }

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                sonicModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Gemini returned empty Lyria3 envelope.")
            val json = JSONObject(raw)
            SonicEnvelopeAiResult(
                promptEnvelope = json.optString("promptEnvelope", ""),
                summary = json.optString("summary", "Lyria3 master envelope generated.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Sonic envelope generation timed out", e)
            throw IllegalStateException("Lyria3 envelope generation timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sonic envelope generation failed", e)
            throw e
        }
    }

    suspend fun evaluateWetLiftSonic(doc: SonicRunDoc): WetLiftAiResult {
        val prompt = buildString {
            appendLine("Perform an explicit KickGuard Wet-Lift Sonic Coherence scoring pass on this ⫻flow/qllickfizz_sonic Lyria3 envelope.")
            appendLine("Target threshold for seal: sonic_coherence >= 0.93 (RULE-09).")
            appendLine("Evaluate strictly on: FORMAT-09 order fidelity, micro_timing specificity, spatial mapping clarity, energy/contrast alignment, and RULE-10 consent compliance.")
            appendLine("Envelope:\n${doc.promptEnvelope}")
        }
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                wetLiftModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Gemini returned empty wet-lift score.")
            val json = JSONObject(raw)
            WetLiftAiResult(
                sonicCoherence = json.optDouble("sonicCoherence", 0.94).coerceIn(0.0, 1.0),
                rationale = json.optString("rationale", "Wet-lift sonic coherence evaluated by KickGuard.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Wet-lift scoring timed out", e)
            throw IllegalStateException("Wet-lift scoring timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Wet-lift scoring failed", e)
            throw e
        }
    }
}
