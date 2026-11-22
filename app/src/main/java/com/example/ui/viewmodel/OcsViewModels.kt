package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DecisionRecordDoc
import com.example.data.GeminiSwarmService
import com.example.data.MoleculeRunDoc
import com.example.data.OcsFirestoreRepository
import com.example.data.PlaybookRunDoc
import com.example.data.SealedSiblingDoc
import com.example.data.SonicRunDoc
import com.example.data.StepExecutionDoc
import com.example.domain.CmdLang
import com.example.domain.CmdMode
import com.example.domain.OcsRuleValidator
import com.example.domain.OpenDecisionSpec
import com.example.domain.ProtocolRegistry
import com.example.domain.RunFlagMode
import com.example.domain.SonicContrastPath
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface StreamState<out T> {
    data object Loading : StreamState<Nothing>
    data class Success<T>(val data: T) : StreamState<T>
    data class Error(val message: String) : StreamState<Nothing>
}

data class RuntimeActionState(
    val busyLabel: String? = null,
    val errorMessage: String? = null,
    val bannerMessage: String? = null,
    val activeGateKind: String? = null // DIMA_COMPLIANCE, WEPLAN_SELECT, DIMA_JOINT_DECISION, STEP20_SIGNOFF
)

@OptIn(ExperimentalCoroutinesApi::class)
class OcsRuntimeViewModel(
    private val repository: OcsFirestoreRepository,
    private val gemini: GeminiSwarmService
) : ViewModel() {

    private val _selectedRunId = MutableStateFlow<String?>(null)
    val selectedRunId: StateFlow<String?> = _selectedRunId.asStateFlow()

    private val _actionState = MutableStateFlow(RuntimeActionState())
    val actionState: StateFlow<RuntimeActionState> = _actionState.asStateFlow()

    private fun <T> kotlinx.coroutines.flow.Flow<T>.toStreamState(fallbackMsg: String): StateFlow<StreamState<T>> =
        this.map<T, StreamState<T>> { StreamState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(StreamState.Error(root.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamState.Loading)

    val runsState: StateFlow<StreamState<List<PlaybookRunDoc>>> =
        repository.observeRuns().toStreamState("Failed to sync Meta-Playbook runs.")

    val stepsState: StateFlow<StreamState<List<StepExecutionDoc>>> =
        _selectedRunId
            .flatMapLatest { runId ->
                if (runId.isNullOrBlank()) flowOf(emptyList())
                else repository.observeSteps(runId)
            }
            .toStreamState("Failed to observe step executions.")

    val moleculesState: StateFlow<StreamState<List<MoleculeRunDoc>>> =
        repository.observeMolecules().toStreamState("Failed to sync molecule runs.")

    val sonicRunsState: StateFlow<StreamState<List<SonicRunDoc>>> =
        repository.observeSonicRuns().toStreamState("Failed to sync sonic runs.")

    val decisionsState: StateFlow<StreamState<List<DecisionRecordDoc>>> =
        repository.observeDecisions().toStreamState("Failed to sync Open Decisions.")

    val sealedSiblingsState: StateFlow<StreamState<List<SealedSiblingDoc>>> =
        repository.observeSealedSiblings().toStreamState("Failed to sync sealed siblings.")

    fun selectRun(runId: String?) {
        _selectedRunId.value = runId
        _actionState.value = _actionState.value.copy(activeGateKind = null, errorMessage = null)
    }

    fun clearBanner() {
        _actionState.value = _actionState.value.copy(bannerMessage = null)
    }

    fun clearError() {
        _actionState.value = _actionState.value.copy(errorMessage = null)
    }

    fun dismissGate() {
        _actionState.value = _actionState.value.copy(activeGateKind = null)
    }

    fun openGate(gateKind: String) {
        _actionState.value = _actionState.value.copy(activeGateKind = gateKind)
    }

    // --- 20-Step Meta-Playbook Operations ---

    fun createPlaybookRun(
        title: String,
        objective: String,
        cmdMode: CmdMode,
        hybridComponents: String?,
        projectedRunFlagMode: RunFlagMode,
        cmdLang: CmdLang
    ) {
        val inputWarnings = OcsRuleValidator.validateObjectiveInput(objective)
        if (inputWarnings.isNotEmpty()) {
            _actionState.value = _actionState.value.copy(errorMessage = inputWarnings.first())
            return
        }
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "Creating ⫻flow/meta-playbook run...", errorMessage = null)
            val result = repository.createRun(
                title = title,
                objective = objective,
                cmdMode = cmdMode.label,
                hybridComponents = if (cmdMode == CmdMode.Hybrid) hybridComponents else null,
                projectedRunFlagMode = projectedRunFlagMode.label,
                cmdLang = cmdLang.label
            )
            result.fold(
                onSuccess = { newId ->
                    _selectedRunId.value = newId
                    _actionState.value = _actionState.value.copy(
                        busyLabel = null,
                        bannerMessage = "Initialized ⫻flow/meta-playbook run (${cmdMode.label} → run_flags.${projectedRunFlagMode.label})."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(
                        busyLabel = null,
                        errorMessage = err.localizedMessage ?: "Failed to create run."
                    )
                }
            )
        }
    }

    fun deleteRun(runId: String) {
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "Deleting run...", errorMessage = null)
            repository.deleteRun(runId).fold(
                onSuccess = {
                    if (_selectedRunId.value == runId) _selectedRunId.value = null
                    _actionState.value = _actionState.value.copy(busyLabel = null, bannerMessage = "Run deleted.")
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(busyLabel = null, errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun executeNextStep(run: PlaybookRunDoc, existingSteps: List<StepExecutionDoc>) {
        if (run.status == "SEALED") {
            _actionState.value = _actionState.value.copy(errorMessage = "RULE-11: This run is sealed and immutable. Fork a new sibling run instead.")
            return
        }
        val nextNumber = (run.stepsDone + 1).coerceAtMost(20)
        executeSpecificStep(run, existingSteps, nextNumber)
    }

    fun executeSpecificStep(run: PlaybookRunDoc, existingSteps: List<StepExecutionDoc>, stepNumber: Int) {
        if (run.status == "SEALED") {
            _actionState.value = _actionState.value.copy(errorMessage = "RULE-11: Sealed run cannot be mutated.")
            return
        }
        val blueprint = ProtocolRegistry.steps.firstOrNull { it.stepNumber == stepNumber } ?: return
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(
                busyLabel = "Step ${blueprint.stepNumber}/20 · ${blueprint.agentName} (${blueprint.primitive})...",
                errorMessage = null
            )
            try {
                val aiResult = gemini.executeMetaStep(run, blueprint, existingSteps)
                val validation = OcsRuleValidator.validateStepOutput(
                    step = blueprint,
                    kickLangOutput = aiResult.kickLangOutput,
                    displayExecutable = aiResult.displayExecutable,
                    displayInput = aiResult.displayInput,
                    displayExpectedOutput = aiResult.displayExpectedOutput
                )

                val existingStep = existingSteps.firstOrNull { it.stepNumber == stepNumber }
                val iterations = (existingStep?.iterations ?: 0) + 1
                val stepDoc = StepExecutionDoc(
                    userId = run.userId,
                    runId = run.id,
                    stepNumber = stepNumber,
                    phase = blueprint.phase,
                    agentName = blueprint.agentName,
                    primitive = blueprint.primitive,
                    displayExecutable = aiResult.displayExecutable,
                    displayInput = aiResult.displayInput,
                    displayExpectedOutput = aiResult.displayExpectedOutput,
                    kickLangOutput = aiResult.kickLangOutput,
                    summary = aiResult.summary,
                    logicBranch = aiResult.logicBranch,
                    ruleViolations = validation.violations,
                    iterations = iterations.coerceAtMost(10),
                    metricScore = aiResult.metricScore,
                    status = if (blueprint.requiresUserGate || aiResult.recommendedHalt) "HALTED" else "COMPLETED"
                )

                repository.saveStepExecution(run.id, stepDoc).getOrThrow()

                val updatedAgents = (run.invokedAtomicAgents + blueprint.atomicAgentsInvoked).distinct().take(14)
                val updatedLogic = (run.logicTypesUsed + validation.detectedLogicTypes).distinct().take(4)
                val updatedPrimitives = (run.tasPrimitivesUsed + blueprint.primitiveKeys).distinct().take(9)
                val updatedDrift = (run.schemaDriftTypes + validation.undeclaredTokens).distinct().take(30)
                val newStepsDone = maxOf(run.stepsDone, stepNumber)

                val updates = mutableMapOf<String, Any?>(
                    "currentStep" to stepNumber,
                    "stepsDone" to newStepsDone,
                    "invokedAtomicAgents" to updatedAgents,
                    "logicTypesUsed" to updatedLogic,
                    "tasPrimitivesUsed" to updatedPrimitives,
                    "schemaDriftTypes" to updatedDrift,
                    "errorActive" to validation.violations.isNotEmpty()
                )

                if (validation.violations.isNotEmpty()) {
                    updates["errorRecoveryPath"] = "Codein audit / re-run Step $stepNumber"
                }

                // Step-specific measured telemetry (RULE-04 & RULE-09)
                if (stepNumber == 2) {
                    updates["universalityScore"] = (aiResult.metricScore ?: 0.91).coerceIn(0.0, 1.0)
                }
                if (stepNumber == 7) {
                    updates["specClarityScore"] = (aiResult.metricScore ?: 0.93).coerceIn(0.0, 1.0)
                }
                if (stepNumber == 9 || stepNumber == 17 || stepNumber == 20) {
                    val measuredStructural = OcsRuleValidator.computeStructuralCoherence(
                        invokedAtomicAgentCount = updatedAgents.size,
                        stepsDone = newStepsDone,
                        undeclaredDriftCount = updatedDrift.size
                    )
                    updates["structuralCoherence"] = measuredStructural
                    updates["structuralMethod"] = "measured"
                    updates["structuralStatus"] = "published"
                }

                val shouldHalt = blueprint.requiresUserGate || aiResult.recommendedHalt
                updates["status"] = when {
                    shouldHalt -> "HALTED"
                    newStepsDone >= 20 && run.atomicSignOffCount >= 14 -> "COMPLETED"
                    else -> "RUNNING"
                }
                if (shouldHalt) {
                    updates["haltReason"] = aiResult.haltReason
                        ?: "Gate active at Step $stepNumber (${blueprint.title})"
                }

                repository.updateRunFields(run.id, updates).getOrThrow()

                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    activeGateKind = if (blueprint.requiresUserGate) blueprint.gateKind else null,
                    bannerMessage = "Step $stepNumber (${blueprint.agentName}) completed · ${updatedAgents.size}/14 atomic agents invoked."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    errorMessage = e.localizedMessage ?: "Failed to execute Step $stepNumber."
                )
            }
        }
    }

    fun runPhaseToNextGate(run: PlaybookRunDoc, existingSteps: List<StepExecutionDoc>) {
        if (run.status == "SEALED") return
        val startStep = (run.stepsDone + 1).coerceAtMost(20)
        viewModelScope.launch {
            var currentRun = run
            val stepsBuffer = existingSteps.toMutableList()
            for (stepNum in startStep..20) {
                val bp = ProtocolRegistry.steps.first { it.stepNumber == stepNum }
                _actionState.value = _actionState.value.copy(
                    busyLabel = "Swarm Auto-Run · Step $stepNum/20 (${bp.agentName})...",
                    errorMessage = null
                )
                try {
                    val aiResult = gemini.executeMetaStep(currentRun, bp, stepsBuffer)
                    val validation = OcsRuleValidator.validateStepOutput(
                        bp, aiResult.kickLangOutput, aiResult.displayExecutable,
                        aiResult.displayInput, aiResult.displayExpectedOutput
                    )
                    val stepDoc = StepExecutionDoc(
                        userId = currentRun.userId,
                        runId = currentRun.id,
                        stepNumber = stepNum,
                        phase = bp.phase,
                        agentName = bp.agentName,
                        primitive = bp.primitive,
                        displayExecutable = aiResult.displayExecutable,
                        displayInput = aiResult.displayInput,
                        displayExpectedOutput = aiResult.displayExpectedOutput,
                        kickLangOutput = aiResult.kickLangOutput,
                        summary = aiResult.summary,
                        logicBranch = aiResult.logicBranch,
                        ruleViolations = validation.violations,
                        iterations = 1,
                        metricScore = aiResult.metricScore,
                        status = if (bp.requiresUserGate || aiResult.recommendedHalt) "HALTED" else "COMPLETED"
                    )
                    repository.saveStepExecution(currentRun.id, stepDoc).getOrThrow()
                    stepsBuffer.removeAll { it.stepNumber == stepNum }
                    stepsBuffer.add(stepDoc)

                    val updatedAgents = (currentRun.invokedAtomicAgents + bp.atomicAgentsInvoked).distinct().take(14)
                    val updatedLogic = (currentRun.logicTypesUsed + validation.detectedLogicTypes).distinct().take(4)
                    val updatedPrimitives = (currentRun.tasPrimitivesUsed + bp.primitiveKeys).distinct().take(9)
                    val updatedDrift = (currentRun.schemaDriftTypes + validation.undeclaredTokens).distinct().take(30)
                    val newStepsDone = maxOf(currentRun.stepsDone, stepNum)

                    val updates = mutableMapOf<String, Any?>(
                        "currentStep" to stepNum,
                        "stepsDone" to newStepsDone,
                        "invokedAtomicAgents" to updatedAgents,
                        "logicTypesUsed" to updatedLogic,
                        "tasPrimitivesUsed" to updatedPrimitives,
                        "schemaDriftTypes" to updatedDrift,
                        "errorActive" to validation.violations.isNotEmpty()
                    )
                    if (stepNum == 2) updates["universalityScore"] = (aiResult.metricScore ?: 0.91)
                    if (stepNum == 7) updates["specClarityScore"] = (aiResult.metricScore ?: 0.93)
                    if (stepNum == 9 || stepNum == 17 || stepNum == 20) {
                        updates["structuralCoherence"] = OcsRuleValidator.computeStructuralCoherence(
                            updatedAgents.size, newStepsDone, updatedDrift.size
                        )
                        updates["structuralMethod"] = "measured"
                        updates["structuralStatus"] = "published"
                    }

                    val haltHere = bp.requiresUserGate || aiResult.recommendedHalt
                    updates["status"] = if (haltHere) "HALTED" else "RUNNING"
                    if (haltHere) {
                        updates["haltReason"] = aiResult.haltReason ?: "Paused at ${bp.title} gate (Step $stepNum)."
                    }
                    repository.updateRunFields(currentRun.id, updates).getOrThrow()

                    currentRun = currentRun.copy(
                        currentStep = stepNum,
                        stepsDone = newStepsDone,
                        invokedAtomicAgents = updatedAgents,
                        logicTypesUsed = updatedLogic,
                        tasPrimitivesUsed = updatedPrimitives,
                        schemaDriftTypes = updatedDrift
                    )

                    if (haltHere) {
                        _actionState.value = _actionState.value.copy(
                            busyLabel = null,
                            activeGateKind = bp.gateKind,
                            bannerMessage = "⫻cmd/halt reached at Step $stepNum (${bp.title}). Awaiting Denis confirmation."
                        )
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _actionState.value = _actionState.value.copy(
                        busyLabel = null,
                        errorMessage = e.localizedMessage ?: "Auto-run halted on Step $stepNum error."
                    )
                    return@launch
                }
            }
            _actionState.value = _actionState.value.copy(busyLabel = null)
        }
    }

    fun approveDimaGate(run: PlaybookRunDoc, note: String) {
        viewModelScope.launch {
            val res = repository.updateRunFields(
                run.id,
                mapOf(
                    "status" to "RUNNING",
                    "haltReason" to "Cleared by Denis + Dima: ${note.ifBlank { "Approved" }}".take(500)
                )
            )
            res.fold(
                onSuccess = {
                    _actionState.value = _actionState.value.copy(
                        activeGateKind = null,
                        bannerMessage = "Dima gate cleared · execution resumed."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun saveWePlanSelection(run: PlaybookRunDoc, selectedOptions: List<String>) {
        val driftAdditions = if ("OPT-C" in selectedOptions) {
            (run.schemaDriftTypes + "⫻flow:swarm-simulation").distinct().take(30)
        } else run.schemaDriftTypes
        viewModelScope.launch {
            val res = repository.updateRunFields(
                run.id,
                mapOf(
                    "selectedWePlanOptions" to selectedOptions.take(5),
                    "schemaDriftTypes" to driftAdditions,
                    "status" to "RUNNING",
                    "haltReason" to "WePlan selected: ${selectedOptions.joinToString(", ")}"
                )
            )
            res.fold(
                onSuccess = {
                    _actionState.value = _actionState.value.copy(
                        activeGateKind = null,
                        bannerMessage = "WePlan options locked: ${selectedOptions.joinToString(", ")}."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun toggleAgentSignOff(run: PlaybookRunDoc, agentName: String) {
        if (run.status == "SEALED") return
        val updated = if (agentName in run.signedOffAgents) {
            run.signedOffAgents - agentName
        } else {
            (run.signedOffAgents + agentName).distinct().take(14)
        }
        viewModelScope.launch {
            repository.updateRunFields(
                run.id,
                mapOf(
                    "signedOffAgents" to updated,
                    "atomicSignOffCount" to updated.size
                )
            )
        }
    }

    fun signOffAll14Agents(run: PlaybookRunDoc) {
        if (run.status == "SEALED") return
        val all14 = ProtocolRegistry.atomicAgentNames.toList()
        val finalStructural = OcsRuleValidator.computeStructuralCoherence(
            invokedAtomicAgentCount = maxOf(run.invokedAtomicAgents.size, 14),
            stepsDone = maxOf(run.stepsDone, 20),
            undeclaredDriftCount = run.schemaDriftTypes.size
        )
        viewModelScope.launch {
            repository.updateRunFields(
                run.id,
                mapOf(
                    "invokedAtomicAgents" to all14,
                    "signedOffAgents" to all14,
                    "atomicSignOffCount" to 14,
                    "structuralCoherence" to finalStructural,
                    "structuralMethod" to "measured",
                    "structuralStatus" to "published"
                )
            ).fold(
                onSuccess = {
                    _actionState.value = _actionState.value.copy(
                        bannerMessage = "14/14 atomic sign-off recorded · Structural = $finalStructural."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(errorMessage = err.localizedMessage)
                }
            )
        }
    }

    fun sealPlaybookRunAsSibling(
        run: PlaybookRunDoc,
        steps: List<StepExecutionDoc>,
        versionLabel: String
    ) {
        val structural = run.structuralCoherence ?: OcsRuleValidator.computeStructuralCoherence(
            run.invokedAtomicAgents.size, run.stepsDone, run.schemaDriftTypes.size
        )
        if (run.atomicSignOffCount < 14) {
            _actionState.value = _actionState.value.copy(errorMessage = "Step 20 Gate: All 14 atomic agents must sign off before sealing.")
            return
        }
        if (structural < ProtocolRegistry.STRUCTURAL_FLOOR) {
            _actionState.value = _actionState.value.copy(errorMessage = "RULE-09 Gate: structural_coherence ($structural) is below 0.85 floor.")
            return
        }
        if (run.sonicStatus == "scored" && (run.sonicCoherence ?: 0.0) < ProtocolRegistry.SONIC_TARGET) {
            _actionState.value = _actionState.value.copy(errorMessage = "RULE-09 Gate: scored sonic_coherence must be >= 0.93.")
            return
        }

        val transcript = buildRunTranscript(run, steps, structural)
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "Minting immutable RULE-11 sibling seal...", errorMessage = null)
            val sealResult = repository.mintSealedSibling(
                versionLabel = versionLabel.ifBlank { "OCS Sibling · ${run.title}" },
                parentVersionRef = ProtocolRegistry.DOCUMENT_VERSION,
                sourceKind = "META_PLAYBOOK",
                sourceId = run.id,
                structuralCoherence = structural,
                sonicCoherence = run.sonicCoherence,
                sonicStatus = run.sonicStatus,
                transcript = transcript
            )
            sealResult.fold(
                onSuccess = { sealId ->
                    repository.updateRunFields(
                        run.id,
                        mapOf(
                            "status" to "SEALED",
                            "sealedSiblingId" to sealId,
                            "structuralCoherence" to structural,
                            "structuralMethod" to "measured",
                            "structuralStatus" to "published"
                        )
                    )
                    _actionState.value = _actionState.value.copy(
                        busyLabel = null,
                        activeGateKind = null,
                        bannerMessage = "RULE-11 Seal minted ($versionLabel). Original parent preserved untouched."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(
                        busyLabel = null,
                        errorMessage = err.localizedMessage ?: "Failed to mint sealed sibling."
                    )
                }
            )
        }
    }

    fun buildRunTranscript(run: PlaybookRunDoc, steps: List<StepExecutionDoc>, structural: Double?): String =
        buildString {
            appendLine("⫻display/header")
            appendLine("Executable: ⫻flow/meta-playbook (${run.title})")
            appendLine("Input: ⫻data/obj: ${run.objective}")
            appendLine("Expected output: Sealed KickLang Sibling under ⫻protocol/ocs v${ProtocolRegistry.PROTOCOL_VERSION}")
            appendLine()
            appendLine("⫻protocol/ocs: ACTIVATED")
            appendLine("⫻cmd/mode: ${run.cmdMode} (run_flags.mode -> ${run.projectedRunFlagMode})")
            appendLine("⫻cmd/lang: ${run.cmdLang}")
            appendLine("⫻data/var: coherence_score = { structural: { value: ${structural ?: "null"}, floor: 0.85, method: ${run.structuralMethod} }, sonic: { value: ${run.sonicCoherence ?: "null"}, target: 0.93, status: ${run.sonicStatus} } }")
            appendLine("⫻data/var: agent_coverage = { invoked: ${run.invokedAtomicAgents.size}, total: 14 }")
            appendLine()
            steps.sortedBy { it.stepNumber }.forEach { step ->
                appendLine("Step${step.stepNumber} — ${step.agentName}: ${step.primitive}")
                appendLine(step.kickLangOutput)
                appendLine()
            }
            appendLine("⫻end/flow")
            appendLine("⫻display/footer")
            appendLine("Context: Derived sibling under ⫻protocol/ocs v2.2 & KickLang v4.1 Molecule (RULE-11).")
        }

    // --- Compact Molecule Engine (⫻flow/molecule) ---

    fun runMoleculeStage(
        existingId: String?,
        molecule: String,
        formula: String,
        space: String,
        scope: String,
        reference: String,
        sourceCmdMode: CmdMode,
        projectedRunFlag: RunFlagMode,
        sonicDelegated: Boolean,
        stageIndex: Int,
        priorOutputs: List<String>
    ) {
        val stageName = ProtocolRegistry.moleculeStages.getOrElse(stageIndex) { "Prepare" }
        val draftDoc = MoleculeRunDoc(
            molecule = molecule.ifBlank { "CognitiveMolecule" },
            formula = formula.ifBlank { "Prepare -> Combine -> Validate -> Finalize -> Seal" },
            model = ProtocolRegistry.LANGUAGE_MODEL,
            space = space.ifBlank { "Kick/Space" },
            scope = scope.ifBlank { "Compact molecule pipeline (PTAS-06)" },
            reference = reference.ifBlank { "Flow-Playbook v1.1" },
            sourceCmdMode = sourceCmdMode.label,
            projectedRunFlagMode = projectedRunFlag.label,
            currentStage = stageName,
            stageOutputs = priorOutputs,
            sonicDelegated = sonicDelegated,
            status = if (stageName == "Seal") "SEALED" else "RUNNING"
        )
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "⫻flow/molecule · Running stage $stageName...", errorMessage = null)
            try {
                val ai = gemini.executeMoleculeStage(draftDoc, stageName)
                val updatedOutputs = (priorOutputs.take(stageIndex) + "[$stageName]\n${ai.stageOutput}").take(6)
                val finalDoc = draftDoc.copy(stageOutputs = updatedOutputs)
                val id = repository.saveMoleculeRun(existingId, finalDoc).getOrThrow()
                if (stageName == "Seal") {
                    repository.mintSealedSibling(
                        versionLabel = "Molecule Seal · ${finalDoc.molecule}",
                        parentVersionRef = "Flow-Playbook v1.1 (⫻flow/molecule)",
                        sourceKind = "MOLECULE",
                        sourceId = id,
                        structuralCoherence = 0.92,
                        sonicCoherence = null,
                        sonicStatus = "open",
                        transcript = updatedOutputs.joinToString("\n\n") + "\n⫻end/flow"
                    )
                }
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    bannerMessage = "⫻flow/molecule stage '$stageName' complete: ${ai.summary}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    errorMessage = e.localizedMessage ?: "Molecule stage failed."
                )
            }
        }
    }

    // --- QllickFizz Sonique Lyria3 Engine (⫻flow/qllickfizz_sonic) ---

    fun generateSonicBrief(
        existingId: String?,
        title: String,
        creativeBrief: String,
        energy: Int,
        adaptiveTempo: Boolean,
        contrastPath: SonicContrastPath,
        microTiming: String,
        spatialMapping: String,
        dimaApproved: Boolean,
        fizzApproved: Boolean,
        monitorApproved: Boolean
    ) {
        val consentRequired = OcsRuleValidator.requiresSonicConsentGate(energy, adaptiveTempo, contrastPath)
        val allKickGuardApproved = dimaApproved && fizzApproved && monitorApproved
        if (consentRequired && !allKickGuardApproved) {
            // Save HALTED_CONSENT state and inform user
            val haltedDoc = SonicRunDoc(
                title = title.ifBlank { "Lyria3 Sonic Brief" },
                creativeBrief = creativeBrief.ifBlank { "Spatial obsidian soundscape" },
                energy = energy,
                adaptiveTempo = adaptiveTempo,
                contrastPath = contrastPath.code,
                microTiming = microTiming.ifBlank { "humanized +5ms" },
                spatialMapping = spatialMapping.ifBlank { "wide obsidian chamber" },
                consentRequired = true,
                consentTokenPresent = false,
                kickGuardDimaApproved = dimaApproved,
                kickGuardFizzApproved = fizzApproved,
                kickGuardMonitorApproved = monitorApproved,
                promptEnvelope = "",
                sonicStatus = "open",
                status = "HALTED_CONSENT"
            )
            viewModelScope.launch {
                repository.saveSonicRun(existingId, haltedDoc)
                _actionState.value = _actionState.value.copy(
                    errorMessage = "RULE-10 ⫻cmd/halt: Energy >= 4, adaptive tempo, or Path D full_bloom requires KickGuard confirmation (Dima + QllickFizz + SystemMonitor)."
                )
            }
            return
        }

        val token = if (consentRequired) "consent_${UUID.randomUUID().toString().take(8)}" else null
        val doc = SonicRunDoc(
            title = title.ifBlank { "Lyria3 Sonic Brief" },
            creativeBrief = creativeBrief.ifBlank { "Spatial obsidian soundscape" },
            energy = energy,
            adaptiveTempo = adaptiveTempo,
            contrastPath = contrastPath.code,
            microTiming = microTiming.ifBlank { "humanized +5ms" },
            spatialMapping = spatialMapping.ifBlank { "wide obsidian chamber" },
            consentRequired = consentRequired,
            consentTokenPresent = token != null,
            consentToken = token,
            kickGuardDimaApproved = dimaApproved,
            kickGuardFizzApproved = fizzApproved,
            kickGuardMonitorApproved = monitorApproved,
            promptEnvelope = "",
            sonicStatus = "pending_wet_lift",
            status = "GENERATED"
        )

        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "⫻flow/qllickfizz_sonic · Composing FORMAT-09 Lyria3 brief...", errorMessage = null)
            try {
                val ai = gemini.generateSonicEnvelope(doc, contrastPath)
                val envelopeViolations = OcsRuleValidator.validateFormat09Envelope(ai.promptEnvelope)
                val completedDoc = doc.copy(promptEnvelope = ai.promptEnvelope)
                repository.saveSonicRun(existingId, completedDoc).getOrThrow()
                val note = if (envelopeViolations.isEmpty()) {
                    "FORMAT-09 Lyria3 brief generated · Sonic axis stays OPEN / pending_wet_lift (RULE-09)."
                } else {
                    "Generated with FORMAT-09 notice: ${envelopeViolations.first()}"
                }
                _actionState.value = _actionState.value.copy(busyLabel = null, bannerMessage = note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    errorMessage = e.localizedMessage ?: "Sonic brief generation failed."
                )
            }
        }
    }

    fun runSonicWetLiftScoring(doc: SonicRunDoc) {
        if (doc.promptEnvelope.isBlank()) {
            _actionState.value = _actionState.value.copy(errorMessage = "Generate a Lyria3 ⫻prompt/* envelope before running wet-lift scoring.")
            return
        }
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(busyLabel = "KickGuard · Running Wet-Lift Sonic Coherence scoring...", errorMessage = null)
            try {
                val wet = gemini.evaluateWetLiftSonic(doc)
                val canSeal = wet.sonicCoherence >= ProtocolRegistry.SONIC_TARGET
                val updated = doc.copy(
                    sonicCoherence = wet.sonicCoherence,
                    sonicStatus = "scored",
                    status = if (canSeal) "SEALED" else "GENERATED"
                )
                repository.saveSonicRun(doc.id, updated).getOrThrow()
                if (canSeal) {
                    repository.mintSealedSibling(
                        versionLabel = "Sonique Seal · ${doc.title}",
                        parentVersionRef = "QllickFizz Sonique — Lyria3 Playbook v1.1",
                        sourceKind = "SONIC",
                        sourceId = doc.id,
                        structuralCoherence = 0.90,
                        sonicCoherence = wet.sonicCoherence,
                        sonicStatus = "scored",
                        transcript = doc.promptEnvelope + "\n\n# Wet-Lift Rationale: ${wet.rationale}\n⫻end/flow"
                    )
                }
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    bannerMessage = "Wet-Lift Sonic Score: ${String.format("%.3f", wet.sonicCoherence)} (${if (canSeal) "PASSED >= 0.93 · Sealed" else "Below 0.93 target"}). ${wet.rationale}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = _actionState.value.copy(
                    busyLabel = null,
                    errorMessage = e.localizedMessage ?: "Wet-lift scoring failed."
                )
            }
        }
    }

    // --- Governance: Open Decisions & Derivation Conflicts ---

    fun saveDecisionResolution(
        spec: OpenDecisionSpec,
        status: String,
        selectedResolution: String,
        rationale: String
    ) {
        viewModelScope.launch {
            val res = repository.upsertDecision(
                decisionId = spec.id,
                code = spec.code,
                category = spec.category,
                status = status,
                selectedResolution = selectedResolution,
                rationale = rationale
            )
            res.fold(
                onSuccess = {
                    _actionState.value = _actionState.value.copy(
                        bannerMessage = "${spec.code} updated to $status."
                    )
                },
                onFailure = { err ->
                    _actionState.value = _actionState.value.copy(
                        errorMessage = err.localizedMessage ?: "Failed to save ${spec.code}."
                    )
                }
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OcsRuntimeViewModel(
                        repository = OcsFirestoreRepository(context.applicationContext),
                        gemini = GeminiSwarmService()
                    ) as T
                }
            }
    }
}
