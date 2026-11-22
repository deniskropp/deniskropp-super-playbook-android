package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlaybookRunDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val objective: String = "",
    val cmdMode: String = "Swarm",
    val hybridComponents: String? = null,
    val projectedRunFlagMode: String = "Swarm",
    val cmdLang: String = "EN",
    val currentStep: Int = 0,
    val stepsDone: Int = 0,
    val status: String = "IDLE",
    val haltReason: String? = null,
    val invokedAtomicAgents: List<String> = emptyList(),
    val logicTypesUsed: List<String> = emptyList(),
    val tasPrimitivesUsed: List<String> = emptyList(),
    val schemaDriftTypes: List<String> = emptyList(),
    val selectedWePlanOptions: List<String> = emptyList(),
    val structuralCoherence: Double? = null,
    val structuralStatus: String = "pending_measurement",
    val structuralMethod: String = "asserted",
    val sonicCoherence: Double? = null,
    val sonicStatus: String = "open",
    val universalityScore: Double? = null,
    val specClarityScore: Double? = null,
    val consentTokenPresent: Boolean = false,
    val consentScope: String? = null,
    val errorActive: Boolean = false,
    val errorRecoveryPath: String? = null,
    val atomicSignOffCount: Int = 0,
    val signedOffAgents: List<String> = emptyList(),
    val sealedSiblingId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StepExecutionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val runId: String = "",
    val stepNumber: Int = 1,
    val phase: Int = 1,
    val agentName: String = "",
    val primitive: String = "",
    val displayExecutable: String = "",
    val displayInput: String = "",
    val displayExpectedOutput: String = "",
    val kickLangOutput: String = "",
    val summary: String = "",
    val logicBranch: String = "none",
    val ruleViolations: List<String> = emptyList(),
    val iterations: Int = 1,
    val metricScore: Double? = null,
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MoleculeRunDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val molecule: String = "",
    val formula: String = "",
    val model: String = "KickLang v4.1 Molecule",
    val space: String = "Kick/Space",
    val scope: String = "",
    val reference: String = "Flow-Playbook v1.1",
    val sourceCmdMode: String = "Swarm",
    val projectedRunFlagMode: String = "Swarm",
    val currentStage: String = "Prepare",
    val stageOutputs: List<String> = emptyList(),
    val sonicDelegated: Boolean = false,
    val status: String = "DRAFT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SonicRunDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val creativeBrief: String = "",
    val energy: Int = 3,
    val adaptiveTempo: Boolean = false,
    val contrastPath: String = "PATH_B_PULSE",
    val microTiming: String = "tight grid",
    val spatialMapping: String = "wide obsidian chamber",
    val consentRequired: Boolean = false,
    val consentTokenPresent: Boolean = false,
    val consentToken: String? = null,
    val kickGuardDimaApproved: Boolean = false,
    val kickGuardFizzApproved: Boolean = false,
    val kickGuardMonitorApproved: Boolean = false,
    val promptEnvelope: String = "",
    val sonicCoherence: Double? = null,
    val sonicStatus: String = "open",
    val status: String = "CONFIGURING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DecisionRecordDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val category: String = "INHERITED",
    val status: String = "OPEN",
    val selectedResolution: String = "",
    val rationale: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SealedSiblingDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val versionLabel: String = "",
    val parentVersionRef: String = "",
    val protocolVersion: String = "2.2",
    val languageModel: String = "KickLang v4.1 Molecule",
    val sourceKind: String = "META_PLAYBOOK",
    val sourceId: String = "",
    val structuralCoherence: Double = 0.0,
    val sonicCoherence: Double? = null,
    val sonicStatus: String = "open",
    val transcript: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
