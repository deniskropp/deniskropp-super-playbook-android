package com.example.domain

enum class CmdMode(val label: String, val description: String) {
    Strict("Strict", "Rules-locked execution; no undeclared improvisation."),
    Creative("Creative", "Exploratory generation within declared schema; gates still bind."),
    Fluid("Fluid", "Adaptive sequencing under Orchestrator / Fizz La Metta, subject to halt gates."),
    Swarm("Swarm", "Parallel multi-agent execution against declared ⫻plan/group targets (default)."),
    Predictive("Predictive", "Forecast-first; WePlan / SystemMonitor emit projected ⫻data/var before commit."),
    Hybrid("Hybrid", "Declared composition of modes (must name active components, e.g. Swarm+Predictive).");

    fun projectToRunFlagMode(overrideForMolecule: RunFlagMode = RunFlagMode.Swarm): RunFlagMode = when (this) {
        Strict -> RunFlagMode.Strict
        Creative -> RunFlagMode.Creative
        Fluid -> RunFlagMode.Fluid
        Swarm -> RunFlagMode.Swarm
        Predictive, Hybrid -> overrideForMolecule
    }

    companion object {
        fun fromNameSafe(raw: String): CmdMode =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) } ?: Swarm
    }
}

enum class RunFlagMode(val label: String) {
    Strict("Strict"),
    Creative("Creative"),
    Fluid("Fluid"),
    Swarm("Swarm");

    companion object {
        fun fromNameSafe(raw: String): RunFlagMode =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: Swarm
    }
}

enum class CmdLang(val label: String) {
    EN("EN"),
    DE("DE"),
    KickLang("KickLang"),
    VeriLog("VeriLog"),
    SystemC("SystemC");

    companion object {
        fun fromNameSafe(raw: String): CmdLang = when (raw.trim().lowercase()) {
            "english", "en" -> EN
            "deutsch", "de" -> DE
            "kicklang" -> KickLang
            "verilog" -> VeriLog
            "systemc" -> SystemC
            else -> EN
        }
    }
}

enum class CoreGroup(val label: String, val memberCount: Int, val corePrimitives: String) {
    OrchestratorApex("Orchestrator (Apex)", 1, "Final seal & ⫻cmd/halt authority above all 3 cores"),
    KickForge("KickForge", 2, "TAS-EXTRACT, TAS-PURIFY, TAS-VALIDATE, TAS-MEASURE (TAS-unit)"),
    KickFlow("KickFlow", 4, "TAS-SEQUENCE, TAS-ALIGN, TAS-DELEGATE (+ member primitives)"),
    KickGuard("KickGuard", 7, "Ethics/consent gates, playbook measurement, rules, NL→formal, visuals")
}

data class OcsAgent(
    val name: String,
    val group: CoreGroup,
    val role: String,
    val signOffContract: String
)

data class TasPrimitiveSpec(
    val name: String,
    val alias: String? = null,
    val description: String,
    val outputPayload: String,
    val dependsOn: List<String> = emptyList()
)

data class MetaStepBlueprint(
    val stepNumber: Int,
    val phase: Int,
    val phaseTitle: String,
    val agentName: String,
    val atomicAgentsInvoked: List<String>,
    val title: String,
    val primitive: String,
    val primitiveKeys: List<String>,
    val inputPayload: String,
    val expectedOutput: String,
    val logicTypes: List<String>,
    val requiresUserGate: Boolean = false,
    val gateKind: String? = null,
    val description: String
)

data class OcsRuleSpec(
    val code: String,
    val owner: String,
    val summary: String,
    val isFormat: Boolean
)

data class WePlanOptionSpec(
    val code: String,
    val target: String,
    val command: String,
    val output: String,
    val statusNote: String,
    val hasSchemaDriftWarning: Boolean = false
)

data class OpenDecisionSpec(
    val id: String,
    val code: String,
    val category: String, // "INHERITED" or "CONFLICT"
    val title: String,
    val summary: String,
    val options: List<String>,
    val defaultDraft: String
)

enum class SonicContrastPath(val code: String, val label: String, val requiresConsent: Boolean) {
    PATH_A_AMBIENT("PATH_A_AMBIENT", "Path A · Ambient Drift", false),
    PATH_B_PULSE("PATH_B_PULSE", "Path B · Structured Pulse", false),
    PATH_C_SURGE("PATH_C_SURGE", "Path C · Kinetic Surge", false),
    PATH_D_FULL_BLOOM("PATH_D_FULL_BLOOM", "Path D · Full Bloom Contrast (RULE-10)", true);

    companion object {
        fun fromCodeSafe(raw: String): SonicContrastPath =
            entries.firstOrNull { it.code == raw } ?: PATH_B_PULSE
    }
}

object ProtocolRegistry {
    const val DOCUMENT_VERSION = "OCS KickLang Super Playbook v1.0.0"
    const val PROTOCOL_VERSION = "2.2"
    const val LANGUAGE_MODEL = "KickLang v4.1 Molecule"
    const val STRUCTURAL_FLOOR = 0.85
    const val SONIC_TARGET = 0.93
    const val DEPRECATED_ALIAS = "SuperGrok Leader"

    val moleculeStages: List<String> = listOf(
        "Prepare", "Combine", "Validate", "Retry", "Finalize", "Seal"
    )

    val declaredSectionPrefixes: Set<String> = setOf(
        "⫻protocol/ocs", "⫻context/", "⫻context/klmx",
        "⫻cmd/exec", "⫻cmd/halt", "⫻cmd/mode", "⫻cmd/lang",
        "⫻data/obj", "⫻data/tas", "⫻data/tas_raw", "⫻data/tas_purified", "⫻data/ptas",
        "⫻data/spec", "⫻data/logic", "⫻data/rv", "⫻data/draft", "⫻data/cite",
        "⫻data/var", "⫻data/rules", "⫻data/visual", "⫻data/state", "⫻data/insight", "⫻data/conflict",
        "⫻prompt/", "⫻role/", "⫻objective/", "⫻constraints/", "⫻task/",
        "⫻output-format/", "⫻evaluation/", "⫻iteration/",
        "⫻logic/if", "⫻logic/else", "⫻logic/loop", "⫻logic/error", "⫻logic/if/error",
        "⫻query/clarify",
        "⫻flow/", "⫻flow/meta-playbook", "⫻flow/qllickfizz_sonic", "⫻flow/molecule",
        "⫻flow:meta-playbook/execution",
        "⫻end/flow", "⫻end/protocol",
        "⫻plan/", "⫻plan/group:All", "⫻plan/group:KickForge", "⫻plan/group:KickFlow", "⫻plan/group:KickGuard",
        "⫻display/", "⫻display/meta", "⫻display/header", "⫻display/footer", "⫻display/body",
        "⫻kicklang:header:"
    )

    val promptFamilyTokens: Set<String> = setOf(
        "⫻role/", "⫻objective/", "⫻constraints/", "⫻task/",
        "⫻output-format/", "⫻evaluation/", "⫻iteration/"
    )

    val agents: List<OcsAgent> = listOf(
        OcsAgent("GPTASe", CoreGroup.KickForge, "TAS Extractor — decomposes objectives into raw TAS units", "Verify TAS extraction completeness"),
        OcsAgent("puTASe", CoreGroup.KickForge, "TAS Purifier — abstracts raw TAS to universal primitives", "Verify universality_score >= 0.85"),
        OcsAgent("Lyra", CoreGroup.KickFlow, "Prompt Engineer / Workflow Structurer — builds ⫻flow & ⫻prompt specs", "Verify ⫻flow structural integrity"),
        OcsAgent("Fizz La Metta", CoreGroup.KickFlow, "Coordinator / Cognitive Flow Control — cognitive load & orchestration", "Verify coordination protocol compliance"),
        OcsAgent("AI Tutor", CoreGroup.KickFlow, "Knowledge Transfer — synthesizes learning between phases/roles", "Verify knowledge transfer completeness"),
        OcsAgent("Codein", CoreGroup.KickFlow, "Code Investigator — logic, structure, and implementation review", "Verify implementation-readiness of ⫻data/logic"),
        OcsAgent("Dima", CoreGroup.KickGuard, "Ethical Compliance / Joint Decisions — safety, integrity & consent gating", "Verify ethical compliance (final gate)"),
        OcsAgent("AR-00L", CoreGroup.KickGuard, "Visual Assets — diagrams, dependency graphs, visual specs", "Verify ⫻data/visual assets bound and rendered"),
        OcsAgent("QllickBuzz", CoreGroup.KickGuard, "Operational Rules — input constraints & triggers (RULE-01..11)", "Verify all input rules satisfied"),
        OcsAgent("QllickFizz", CoreGroup.KickGuard, "Operational Rules — output formatting & schema enforcement (FORMAT-01..10)", "Verify all output format rules satisfied"),
        OcsAgent("WePlan", CoreGroup.KickGuard, "Strategic / Tailored Plans — roadmaps and next-action specs", "Verify strategic plan is actionable"),
        OcsAgent("Kick La Metta", CoreGroup.KickGuard, "NL to Formal Translation — prose → KickLang syntax", "Verify full NL→Formal translation coverage"),
        OcsAgent("SystemMonitor", CoreGroup.KickGuard, "Integrity Monitoring — coherence, schema, and coverage metrics", "Measure and publish final ⫻data/var state"),
        OcsAgent("Orchestrator", CoreGroup.OrchestratorApex, "Dynamic Role Adaptation / Holistic Approach / Swarm Coordination", "Verify swarm coherence (holistic sign-off)")
    )

    val atomicAgentNames: Set<String> = agents.map { it.name }.toSet()

    val tasPrimitives: List<TasPrimitiveSpec> = listOf(
        TasPrimitiveSpec("TAS-EXTRACT", null, "Decompose high-level goal into atomic, trans-domain action units.", "⫻data/tas_raw", emptyList()),
        TasPrimitiveSpec("TAS-SEQUENCE", null, "Order and link extracted units into coherent operational flows.", "⫻data/spec", listOf("TAS-EXTRACT")),
        TasPrimitiveSpec("TAS-ALIGN", null, "Synchronize actions and sub-goals across temporal/cognitive scales.", "⫻data/spec", listOf("TAS-SEQUENCE")),
        TasPrimitiveSpec("TAS-TRANSFORM", null, "Synthesize information and propagate state transformations across roles.", "⫻data/logic | ⫻data/draft", listOf("TAS-CONTEXTUALIZE")),
        TasPrimitiveSpec("TAS-CONTEXTUALIZE", null, "Anchor each unit to its broader operational environment and state.", "⫻data/var | ⫻data/state", listOf("TAS-DELEGATE")),
        TasPrimitiveSpec("TAS-REFINE", "TAS-PURIFY", "Abstract, simplify, and iteratively purify the TAS repertoire.", "⫻data/tas_purified → ⫻data/ptas", listOf("TAS-VALIDATE")),
        TasPrimitiveSpec("TAS-VALIDATE", null, "Verify universality, correctness, and completeness; gate downstream steps.", "⫻data/var | ⫻logic/error", listOf("TAS-MEASURE")),
        TasPrimitiveSpec("TAS-MEASURE", null, "Quantify system state: coherence, coverage, drift, convergence.", "⫻data/var (measured)", listOf("TAS-EXTRACT")),
        TasPrimitiveSpec("TAS-DELEGATE", null, "Assign sub-tasks to atomic agents or declared ⫻plan/group aliases.", "⫻cmd/exec", listOf("TAS-ALIGN"))
    )

    val steps: List<MetaStepBlueprint> = listOf(
        MetaStepBlueprint(1, 1, "Extraction & Purification", "GPTASe", listOf("GPTASe"), "TAS Extraction", "TAS-EXTRACT", listOf("TAS-EXTRACT"), "⫻data/obj", "⫻data/tas_raw", listOf("error"), false, null, "Decompose ⫻data/obj into >=3 atomic action units; error halts for clarification if <3."),
        MetaStepBlueprint(2, 1, "Extraction & Purification", "puTASe", listOf("puTASe"), "TAS Purification", "TAS-PURIFY (alias TAS-REFINE)", listOf("TAS-REFINE"), "⫻data/tas_raw", "⫻data/tas_purified", listOf("loop", "error"), false, null, "Abstract raw TAS into universal primitives until universality_score >= 0.85 (max 5 loops)."),
        MetaStepBlueprint(3, 2, "Structuring & Coordination", "Lyra", listOf("Lyra"), "Workflow Structuring", "TAS-SEQUENCE / TAS-ALIGN", listOf("TAS-SEQUENCE", "TAS-ALIGN"), "⫻data/tas_purified", "⫻data/spec", listOf("if", "else", "error"), false, null, "Build ordered ⫻flow with dependency graph, branches, and optional recursive sub-flows."),
        MetaStepBlueprint(4, 2, "Structuring & Coordination", "Fizz La Metta", listOf("Fizz La Metta"), "Cognitive Coordination", "TAS-DELEGATE", listOf("TAS-DELEGATE"), "⫻data/spec", "⫻cmd/exec", listOf("if", "else", "error"), false, null, "Distribute sub-tasks across active swarm agents while managing cognitive load."),
        MetaStepBlueprint(5, 2, "Structuring & Coordination", "AI Tutor", listOf("AI Tutor"), "Knowledge Transfer", "TAS-TRANSFORM", listOf("TAS-TRANSFORM"), "⫻data/tas_purified + ⫻data/spec", "⫻data/logic", listOf("error"), false, null, "Bridge Phase 1 outputs to Phase 2 consumers with annotated transformation notes."),
        MetaStepBlueprint(6, 2, "Structuring & Coordination", "Codein", listOf("Codein"), "Code Context Investigation", "TAS-CONTEXTUALIZE", listOf("TAS-CONTEXTUALIZE"), "⫻data/spec", "⫻data/logic + ⫻data/state", listOf("error"), false, null, "Bind ⫻data/spec to implementation environment, flag schema_drift and missing error handlers."),
        MetaStepBlueprint(7, 3, "Refinement & Protocol", "Lyra", listOf("Lyra"), "Prompt Refinement", "TAS-REFINE", listOf("TAS-REFINE"), "⫻data/spec + Codein report", "⫻data/spec (v2)", listOf("loop", "error"), false, null, "Iteratively refine ⫻data/spec with AI Tutor annotations until spec_clarity_score >= 0.90."),
        MetaStepBlueprint(8, 3, "Refinement & Protocol", "Fizz La Metta", listOf("Fizz La Metta"), "Protocol Establishment", "TAS-SEQUENCE", listOf("TAS-SEQUENCE"), "⫻cmd/mode + session state", "⫻cmd/mode + run_flags.mode", emptyList(), false, null, "Lock global ⫻cmd/mode and project into run_flags.mode ∈ {Strict, Creative, Fluid, Swarm}."),
        MetaStepBlueprint(9, 3, "Refinement & Protocol", "SystemMonitor", listOf("SystemMonitor"), "Interim System Measurement", "TAS-MEASURE", listOf("TAS-MEASURE"), "agent_coverage + schema_drift", "⫻data/var (interim structural)", listOf("if", "else"), false, null, "Checkpoint structural_coherence (>= 0.70 to proceed); keep sonic axis OPEN unless wet-lift scored."),
        MetaStepBlueprint(10, 3, "Refinement & Protocol", "Dima", listOf("Dima"), "Ethical Compliance", "TAS-VALIDATE", listOf("TAS-VALIDATE"), "Steps 1–9 outputs", "⫻cmd/halt | continue", listOf("error"), true, "DIMA_COMPLIANCE", "Run ethical compliance checklist, verify citations, and queue unresolved ⫻data/conflict items."),
        MetaStepBlueprint(11, 3, "Refinement & Protocol", "AR-00L", listOf("AR-00L"), "Visual Assets (Required)", "TAS-TRANSFORM", listOf("TAS-TRANSFORM"), "Live flow & TAS state", "⫻data/visual (VISUAL-01..06)", listOf("error"), false, null, "Bind required visual specs VISUAL-01..VISUAL-06 directly to flow nodes and telemetry."),
        MetaStepBlueprint(12, 3, "Refinement & Protocol", "QllickBuzz & QllickFizz", listOf("QllickBuzz", "QllickFizz"), "Operational Rules Enforcement", "TAS-VALIDATE", listOf("TAS-VALIDATE"), "Steps 1–11 outputs", "⫻cmd/mode (rules-locked)", listOf("error"), false, null, "Audit outputs against RULE-01..11 and FORMAT-01..10; flag violations in schema_drift."),
        MetaStepBlueprint(13, 4, "Planning & Translation", "WePlan", listOf("WePlan"), "Strategic Planning", "TAS-SEQUENCE", listOf("TAS-SEQUENCE"), "Validated outputs", "⫻data/spec (OPT-A..OPT-E)", emptyList(), true, "WEPLAN_SELECT", "Generate formal KickLang strategic roadmap options OPT-A through OPT-E for user selection."),
        MetaStepBlueprint(14, 4, "Planning & Translation", "Kick La Metta", listOf("Kick La Metta"), "NL to Formal Translation", "TAS-TRANSFORM", listOf("TAS-TRANSFORM"), "Prose from Steps 1–13", "⫻data/ptas", listOf("if", "else", "error"), false, null, "Translate all NL prose into canonical formally-encoded KickLang syntax without semantic drift."),
        MetaStepBlueprint(15, 5, "Adaptation & Synthesis", "Orchestrator", listOf("Orchestrator"), "Dynamic Role Adaptation", "TAS-DELEGATE + TAS-ALIGN", listOf("TAS-DELEGATE", "TAS-ALIGN"), "Current swarm state", "⫻flow (updated) + ⫻data/state", listOf("if", "else", "error"), false, null, "Re-assess load distribution, re-route stalled steps, and coordinate domain engines (PTAS-06)."),
        MetaStepBlueprint(16, 5, "Adaptation & Synthesis", "Dima", listOf("Dima"), "Joint Decisions (RULE-06 Pre-Halt)", "TAS-VALIDATE", listOf("TAS-VALIDATE"), "Contested choices & ⫻data/conflict", "⫻cmd/halt | ⫻data/logic", listOf("error"), true, "DIMA_JOINT_DECISION", "Mandatory pre-gate ⫻cmd/halt for human-in-the-loop resolution of contested specs or conflicts."),
        MetaStepBlueprint(17, 5, "Adaptation & Synthesis", "SystemMonitor", listOf("SystemMonitor"), "Full Integrity Audit", "TAS-MEASURE", listOf("TAS-MEASURE"), "All 16 prior steps", "⫻data/var (measured audit)", listOf("if", "else"), false, null, "Compute structural_coherence = (invoked/14)*(steps/20)*schema_purity >= 0.85; verify 4 logic types & 9 TAS."),
        MetaStepBlueprint(18, 5, "Adaptation & Synthesis", "WePlan", listOf("WePlan"), "Tailored Plans", "TAS-SEQUENCE / TAS-ALIGN", listOf("TAS-SEQUENCE", "TAS-ALIGN"), "Selected OPT-A..OPT-E", "⫻data/spec (tailored plan)", listOf("if", "else", "error"), false, null, "Build execution-ready single or parallel-merged tailored plan from selected options."),
        MetaStepBlueprint(19, 6, "Holistic Synthesis & Review", "Orchestrator", listOf("Orchestrator"), "Holistic Task Approach", "TAS-ALIGN + TAS-CONTEXTUALIZE", listOf("TAS-ALIGN", "TAS-CONTEXTUALIZE"), "All ⫻data/* artifacts", "⫻flow (master) + ⫻data/insight", emptyList(), false, null, "Synthesize all phases into a coherent, contradiction-free master playbook and final insight."),
        MetaStepBlueprint(20, 6, "Holistic Synthesis & Review", "All Agents (14)", atomicAgentNames.toList(), "Review, Sign-Off & Seal", "TAS-REFINE + TAS-VALIDATE", listOf("TAS-REFINE", "TAS-VALIDATE"), "Master ⫻flow & ⫻data/var", "⫻data/ptas v2.2 FROZEN + ⫻end/flow", listOf("if", "else"), true, "STEP20_SIGNOFF", "Parallel atomic 14-agent sign-off, structural >= 0.85 check, and immutable RULE-11 sibling seal.")
    )

    val rules: List<OcsRuleSpec> = listOf(
        OcsRuleSpec("RULE-01", "QllickBuzz", "Every ⫻cmd/exec must reference a declared agent or ⫻plan/group target. 'SuperGrok Leader' is forbidden.", false),
        OcsRuleSpec("RULE-02", "QllickBuzz", "Every ⫻data/* payload must be declared in ⫻protocol/ocs before use.", false),
        OcsRuleSpec("RULE-03", "QllickBuzz", "Every ⫻logic/loop needs ⫻logic/error; every ⫻logic/if needs else, error, or halt.", false),
        OcsRuleSpec("RULE-04", "QllickBuzz", "Coherence metrics use method: measured only after real execution; never invent scores or fill OPEN sonic.", false),
        OcsRuleSpec("RULE-05", "QllickBuzz", "No agent may self-report 100% completion without SystemMonitor sign-off.", false),
        OcsRuleSpec("RULE-06", "QllickBuzz", "⫻cmd/halt is mandatory before any Dima joint-decision gate (Step 16) and on compliance violations.", false),
        OcsRuleSpec("RULE-07", "QllickBuzz", "All NL prose outputs must have a corresponding Kick La Metta formal pass.", false),
        OcsRuleSpec("RULE-08", "QllickBuzz", "Every ⫻ section type must be declared; Prompt-family types only inside ⫻prompt/* envelopes.", false),
        OcsRuleSpec("RULE-09", "QllickBuzz", "Dual-axis coherence: structural >= 0.85 global floor; sonic OPEN until wet-lift then 0.93. Never collapse axes.", false),
        OcsRuleSpec("RULE-10", "QllickBuzz", "Domain consent gates (Sonique energy >= 4, adaptive tempo, Path D full_bloom) require ⫻cmd/halt + KickGuard + consent_token.", false),
        OcsRuleSpec("RULE-11", "QllickBuzz", "Sealed-parent / sibling-version: never mutate a sealed version in place; ship changes as a new sibling.", false),
        OcsRuleSpec("FORMAT-01", "QllickFizz", "Flow steps use StepN — AgentName: Description with ⫻cmd/exec, Action, and Output lines.", true),
        OcsRuleSpec("FORMAT-02", "QllickFizz", "Measured metric ⫻data/var entries include value & method; pending uses value: null.", true),
        OcsRuleSpec("FORMAT-03", "QllickFizz", "TAS units must include name, description, and output payload reference.", true),
        OcsRuleSpec("FORMAT-04", "QllickFizz", "Visual assets (AR-00L) must be listed in ⫻data/visual with spec type.", true),
        OcsRuleSpec("FORMAT-05", "QllickFizz", "WePlan roadmaps encode options as formal ⫻data/spec, not prose lists.", true),
        OcsRuleSpec("FORMAT-06", "QllickFizz", "Error paths specify recovery_path or escalate to ⫻cmd/halt.", true),
        OcsRuleSpec("FORMAT-07", "QllickFizz", "Every ⫻flow block must end with a convergence condition or ⫻cmd/halt.", true),
        OcsRuleSpec("FORMAT-08", "QllickFizz", "Every response to Denis begins with ⫻display/header (Executable, Input, Expected output).", true),
        OcsRuleSpec("FORMAT-09", "QllickFizz", "⫻prompt/* envelope order: prompt, role, objective, context, constraints, task, output-format, evaluation, iteration.", true),
        OcsRuleSpec("FORMAT-10", "QllickFizz", "Every ⫻flow block terminates with ⫻end/flow or ⫻cmd/halt.", true)
    )

    val wePlanOptions: List<WePlanOptionSpec> = listOf(
        WePlanOptionSpec("OPT-A", "universality_score > 0.95 (5 domains)", "⫻cmd/exec puTASe → TAS-REFINE / TAS-PURIFY", "Cross-context validated ⫻data/ptas", "Follow-on purification pass across 5 distinct domains."),
        WePlanOptionSpec("OPT-B", "Full KickLang syntax implementation", "⫻cmd/exec Codein, Lyra → ⫻data/logic", "Executable playbook with annotated examples", "Bind formal KickLang syntax into runnable implementation artifacts."),
        WePlanOptionSpec("OPT-C", "Live TAS demonstration on user goal", "⫻cmd/exec Orchestrator → ⫻flow:swarm-simulation", "Live swarm trace for ⫻data/obj", "SP-G Drift Notice: ⫻flow:swarm-simulation is undeclared until registered in a sibling.", hasSchemaDriftWarning = true),
        WePlanOptionSpec("OPT-D", "Merge TAS layer into Master Playbook (v3.0)", "⫻cmd/exec All → ⫻flow:meta-playbook/execution Step 20", "Unified ⫻protocol/ocs v3.0 (reserved)", "PARTIALLY EXECUTED in v2.2; v3.0 remains reserved pending measured Step 20 + Denis seal."),
        WePlanOptionSpec("OPT-E", "Extended domain primitives (TAS-EVOLVE)", "⫻cmd/exec GPTASe → ⫻data/tas_raw", "Candidates: TAS-VALIDATE+, TAS-MEASURE+, TAS-EVOLVE", "TAS-PURIFY remains an alias of TAS-REFINE pending Denis confirmation.")
    )

    val openDecisions: List<OpenDecisionSpec> = listOf(
        OpenDecisionSpec("OD_01", "OD-01", "INHERITED", "Version Numbering & Label Display", "Confirm document v2.2 / Super Playbook v1.0.0, protocol v2.2, KickLang v4.1 Molecule, OPT-D partial, v3.0 reserved.", listOf("Keep 4 identifiers distinct; v3.0 reserved (Draft)", "Use single stacked version label", "Promote protocol to v3.0 immediately"), "Keep 4 identifiers distinct; v3.0 reserved (Draft)"),
        OpenDecisionSpec("OD_02", "OD-02", "INHERITED", "TAS-PURIFY Alias vs 10th Primitive", "Flow-Playbook v1.1 names TAS-PURIFY; v2.1 canonical slot is TAS-REFINE (9 primitives).", listOf("Keep TAS-PURIFY as alias of TAS-REFINE (Draft)", "Rename canonical primitive to TAS-PURIFY", "Add TAS-PURIFY as 10th distinct primitive"), "Keep TAS-PURIFY as alias of TAS-REFINE (Draft)"),
        OpenDecisionSpec("OD_03", "OD-03", "INHERITED", "Three-Core Partition (2+4+7+1)", "Confirm Flow-Playbook v1.1 mapping: KickForge(2), KickFlow(4), KickGuard(7), Orchestrator on top.", listOf("Confirm 2+4+7+1 Flow-Playbook partition (Draft)", "Revert KickGuard to 3-agent Sonique shorthand", "Rebalance agent groups in next sibling"), "Confirm 2+4+7+1 Flow-Playbook partition (Draft)"),
        OpenDecisionSpec("OD_04", "OD-04", "INHERITED", "Dual-Axis Coherence & Wet-Lift Definition", "Structural floor 0.85; sonic OPEN until wet-lift then 0.93. Formal definition of wet-lift is missing in parents.", listOf("Confirm dual-axis + define wet-lift criteria (Draft)", "Require audio stem audit as wet-lift", "Keep sonic axis manually gated per run"), "Confirm dual-axis + define wet-lift criteria (Draft)"),
        OpenDecisionSpec("OD_05", "OD-05", "INHERITED", "Mode Set Projection (6 → 4)", "Protocol ⫻cmd/mode has 6 values; Flow-Playbook run_flags.mode has 4. Step 8 projects Predictive/Hybrid.", listOf("Keep 6-mode protocol + Step 8 projection (Draft)", "Extend run_flags.mode to all 6 values", "Drop Predictive and Hybrid from protocol"), "Keep 6-mode protocol + Step 8 projection (Draft)"),
        OpenDecisionSpec("OD_06", "OD-06", "INHERITED", "Molecule Token, Stage Count & PTAS-06", "Draft token ⫻flow/molecule lists 6 names (Prepare..Seal) while called a 5-stage runtime; confirm PTAS-06.", listOf("Confirm ⫻flow/molecule; Retry=branch, 5 stages + Seal (Draft)", "Formalize as 6 primary stages", "Rename token in next sibling"), "Confirm ⫻flow/molecule; Retry=branch, 5 stages + Seal (Draft)"),
        OpenDecisionSpec("OD_07", "OD-07", "INHERITED", "Prompt Family First-Class vs Sugar", "Draft declares Prompt-family tokens first-class inside ⫻prompt/* envelopes (FORMAT-09).", listOf("Keep Prompt family first-class inside ⫻prompt/* (Draft)", "Treat as syntactic sugar over ⫻data/*"), "Keep Prompt family first-class inside ⫻prompt/* (Draft)"),
        OpenDecisionSpec("OD_08", "OD-08", "INHERITED", "Compatibility Marker ⫻kicklang:header:", "Historical root-prompt session-start marker vs FORMAT-08 ⫻display/header only.", listOf("Keep as session-start compatibility alias (Draft)", "Retire in favour of FORMAT-08 ⫻display/header only"), "Keep as session-start compatibility alias (Draft)"),
        OpenDecisionSpec("OD_09", "OD-09", "INHERITED", "⫻cmd/lang Scope (VeriLog & SystemC)", "Confirm EN | DE | KickLang | VeriLog | SystemC language set.", listOf("Keep full 5-language set including HDL (Draft)", "Scope down to EN | DE | KickLang"), "Keep full 5-language set including HDL (Draft)"),
        OpenDecisionSpec("OD_10", "OD-10", "INHERITED", "⫻data/conflict & ⫻logic/if/error", "Confirm ⫻data/conflict feeding Step 16 and ⫻logic/if/error as inline shorthand (not 5th logic type).", listOf("Confirm both declarations (Draft)", "Promote ⫻logic/if/error to 5th logic type"), "Confirm both declarations (Draft)"),
        OpenDecisionSpec("OD_11", "OD-11", "INHERITED", "Inherited v2.1 Metrics (0.97 / 0.91)", "Keep v2.1 figures strictly as historical citations of sealed parent, never re-asserted.", listOf("Keep as historical citation only (Draft)", "Archive to lineage log only"), "Keep as historical citation only (Draft)"),
        OpenDecisionSpec("OD_12", "OD-12", "INHERITED", "Follow-Up Sibling Restamps", "Should Sonique v1.1 and OCS Root Prompt get additional formal sibling restamp files?", listOf("Defer until Flow-Playbook v1.1 body ingested (Draft)", "Issue Sonique v1.2 sibling restamp now"), "Defer until Flow-Playbook v1.1 body ingested (Draft)"),
        OpenDecisionSpec("SP_A", "SP-A", "CONFLICT", "Super Playbook v1.0.0 vs OPT-D Master Playbook", "Derived unification sibling under v2.2 vs completed OPT-D v3.0 Master Playbook merge.", listOf("Treat as derived sibling under v2.2; OPT-D partial (Draft)", "Promote to Master Playbook v3.0 after Step 20 run"), "Treat as derived sibling under v2.2; OPT-D partial (Draft)"),
        OpenDecisionSpec("SP_B", "SP-B", "CONFLICT", "Undeclared TAS-FORECAST Token", "Not declared in parents; treated in session as Predictive-mode alias.", listOf("Keep TAS-FORECAST as Predictive-mode alias (Draft)", "Register TAS-FORECAST as new primitive in sibling", "Reject TAS-FORECAST name"), "Keep TAS-FORECAST as Predictive-mode alias (Draft)"),
        OpenDecisionSpec("SP_C", "SP-C", "CONFLICT", "⫻data/obj Gloss Wording", "Root prompt adds 'creative intent, shared goal, global direction' beyond Meta-Playbook's 'high-level objective'.", listOf("Union both glosses in next sibling", "Keep Meta-Playbook terse gloss", "Keep both recorded separately (Draft)"), "Keep both recorded separately (Draft)"),
        OpenDecisionSpec("SP_D", "SP-D", "CONFLICT", "⫻context/... Sonic Spatial Mapping", "'Sonic spatial mapping' appears in Root Prompt gloss only, absent in Meta-Playbook schema gloss.", listOf("Include sonic spatial mapping in schema gloss", "Scope sonic spatial mapping to ⫻flow/qllickfizz_sonic only", "Keep both recorded (Draft)"), "Keep both recorded (Draft)"),
        OpenDecisionSpec("SP_E", "SP-E", "CONFLICT", "Prompt-Family Enclosure Clause", "Root prompt requires ⫻prompt/* strictly; Meta-Playbook RULE-08 also permits domain playbook template envelopes.", listOf("Allow domain playbook templates per RULE-08 (Draft)", "Restrict strictly to explicit ⫻prompt/* wrappers"), "Allow domain playbook templates per RULE-08 (Draft)"),
        OpenDecisionSpec("SP_F", "SP-F", "CONFLICT", "⫻data/tas vs ⫻data/tas_raw / tas_purified", "20-step flow writes tas_raw & tas_purified; exact parent/synonym role of bare ⫻data/tas is undefined.", listOf("Define ⫻data/tas as abstract parent of tas_raw/purified", "Deprecate bare ⫻data/tas in favour of explicit stages", "Leave recorded as open (Draft)"), "Leave recorded as open (Draft)"),
        OpenDecisionSpec("SP_G", "SP-G", "CONFLICT", "Undeclared ⫻flow:swarm-simulation & Colon Form", "OPT-C calls undeclared ⫻flow:swarm-simulation; master flow uses both slash and colon forms.", listOf("Register ⫻flow/swarm-simulation & normalize to slash", "Block OPT-C until registered; keep dual typography noted (Draft)"), "Block OPT-C until registered; keep dual typography noted (Draft)"),
        OpenDecisionSpec("SP_H", "SP-H", "CONFLICT", "Step-Local Undeclared State Variables", "spec_clarity_score, nesting_required, agent_load, step_completion, schema_purity, tas_completeness used without schema declaration.", listOf("Promote all 6 into formal ⫻data/var schema in next sibling", "Classify as step-local ephemeral vars (Draft)"), "Classify as step-local ephemeral vars (Draft)")
    )
}
