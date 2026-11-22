package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OcsFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun userRoot(uid: String) = db.collection("users").document(uid)

    // --- Real-Time Observations ---

    fun observeRuns(): Flow<List<PlaybookRunDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/runs"
        emitAll(
            userRoot(uid).collection("runs")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PlaybookRunDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    fun observeSteps(runId: String): Flow<List<StepExecutionDoc>> = flow {
        val uid = requireUserId()
        require(runId.isNotBlank()) { "runId must not be blank" }
        val path = "users/$uid/runs/$runId/steps"
        emitAll(
            userRoot(uid).collection("runs").document(runId).collection("steps")
                .orderBy("stepNumber", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StepExecutionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    fun observeMolecules(): Flow<List<MoleculeRunDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/molecules"
        emitAll(
            userRoot(uid).collection("molecules")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MoleculeRunDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    fun observeSonicRuns(): Flow<List<SonicRunDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/sonicRuns"
        emitAll(
            userRoot(uid).collection("sonicRuns")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SonicRunDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    fun observeDecisions(): Flow<List<DecisionRecordDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/decisions"
        emitAll(
            userRoot(uid).collection("decisions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DecisionRecordDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    fun observeSealedSiblings(): Flow<List<SealedSiblingDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/sealedSiblings"
        emitAll(
            userRoot(uid).collection("sealedSiblings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SealedSiblingDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFlowError(uid, path)
        )
    }

    private fun <T> Flow<T>.handleFlowError(uid: String, path: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    // --- Playbook Runs ---

    suspend fun createRun(
        title: String,
        objective: String,
        cmdMode: String,
        hybridComponents: String?,
        projectedRunFlagMode: String,
        cmdLang: String
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/runs"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120).ifBlank { "Meta-Playbook Run" }
            val cleanObj = objective.trim().take(2000)
            require(cleanObj.isNotBlank()) { "Objective (⫻data/obj) cannot be empty." }

            val docRef = userRoot(uid).collection("runs").document()
            docPath = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "objective" to cleanObj,
                "cmdMode" to cmdMode,
                "hybridComponents" to hybridComponents?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
                "projectedRunFlagMode" to projectedRunFlagMode,
                "cmdLang" to cmdLang,
                "currentStep" to 0,
                "stepsDone" to 0,
                "status" to "IDLE",
                "invokedAtomicAgents" to emptyList<String>(),
                "logicTypesUsed" to emptyList<String>(),
                "tasPrimitivesUsed" to emptyList<String>(),
                "schemaDriftTypes" to emptyList<String>(),
                "selectedWePlanOptions" to emptyList<String>(),
                "structuralStatus" to "pending_measurement",
                "structuralMethod" to "asserted",
                "sonicStatus" to "open",
                "consentTokenPresent" to false,
                "errorActive" to false,
                "atomicSignOffCount" to 0,
                "signedOffAgents" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRunFields(runId: String, updates: Map<String, Any?>): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/runs/$runId"
        return try {
            val uid = requireUserId()
            require(runId.isNotBlank()) { "runId cannot be blank" }
            val docRef = userRoot(uid).collection("runs").document(runId)
            docPath = docRef.path
            val payload = updates.toMutableMap().apply {
                this["updatedAt"] = FieldValue.serverTimestamp()
            }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveStepExecution(runId: String, step: StepExecutionDoc): Result<Unit> {
        val stepDocId = "step_${step.stepNumber}"
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/runs/$runId/steps/$stepDocId"
        return try {
            val uid = requireUserId()
            val stepRef = userRoot(uid).collection("runs").document(runId)
                .collection("steps").document(stepDocId)
            docPath = stepRef.path
            val existing = stepRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any?>(
                    "kickLangOutput" to step.kickLangOutput.take(20000).ifBlank { "⫻data/logic: empty" },
                    "summary" to step.summary.take(1000).ifBlank { "Step executed." },
                    "logicBranch" to step.logicBranch.take(100),
                    "ruleViolations" to step.ruleViolations.take(20),
                    "iterations" to step.iterations.coerceIn(1, 10),
                    "metricScore" to step.metricScore?.coerceIn(0.0, 1.0),
                    "status" to step.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                stepRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "runId" to runId,
                    "stepNumber" to step.stepNumber.coerceIn(1, 20),
                    "phase" to step.phase.coerceIn(1, 6),
                    "agentName" to step.agentName.take(60).ifBlank { "Orchestrator" },
                    "primitive" to step.primitive.take(60).ifBlank { "TAS-TRANSFORM" },
                    "displayExecutable" to step.displayExecutable.take(200).ifBlank { step.primitive },
                    "displayInput" to step.displayInput.take(400).ifBlank { "⫻data/obj" },
                    "displayExpectedOutput" to step.displayExpectedOutput.take(300).ifBlank { "⫻data/spec" },
                    "kickLangOutput" to step.kickLangOutput.take(20000).ifBlank { "⫻data/logic: executed" },
                    "summary" to step.summary.take(1000).ifBlank { "Step completed." },
                    "logicBranch" to step.logicBranch.take(100),
                    "ruleViolations" to step.ruleViolations.take(20),
                    "iterations" to step.iterations.coerceIn(1, 10),
                    "metricScore" to step.metricScore?.coerceIn(0.0, 1.0),
                    "status" to step.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                stepRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRun(runId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/runs/$runId"
        return try {
            val uid = requireUserId()
            val runRef = userRoot(uid).collection("runs").document(runId)
            docPath = runRef.path
            val stepDocs = runRef.collection("steps").get().await()
            for (stepDoc in stepDocs.documents) {
                stepDoc.reference.delete().await()
            }
            runRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Molecule Runs ---

    suspend fun saveMoleculeRun(existingId: String?, doc: MoleculeRunDoc): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/molecules"
        return try {
            val uid = requireUserId()
            val col = userRoot(uid).collection("molecules")
            if (existingId.isNullOrBlank()) {
                val ref = col.document()
                docPath = ref.path
                val payload = mapOf(
                    "userId" to uid,
                    "molecule" to doc.molecule.trim().take(120).ifBlank { "UntitledMolecule" },
                    "formula" to doc.formula.trim().take(300).ifBlank { "Prepare -> Combine -> Validate -> Seal" },
                    "model" to doc.model.trim().take(100).ifBlank { "KickLang v4.1 Molecule" },
                    "space" to doc.space.trim().take(120).ifBlank { "Kick/Space" },
                    "scope" to doc.scope.trim().take(300).ifBlank { "Domain molecule execution" },
                    "reference" to doc.reference.trim().take(300).ifBlank { "Flow-Playbook v1.1" },
                    "sourceCmdMode" to doc.sourceCmdMode,
                    "projectedRunFlagMode" to doc.projectedRunFlagMode,
                    "currentStage" to doc.currentStage,
                    "stageOutputs" to doc.stageOutputs.take(6),
                    "sonicDelegated" to doc.sonicDelegated,
                    "status" to doc.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(existingId)
                docPath = ref.path
                val updates = mapOf(
                    "molecule" to doc.molecule.trim().take(120).ifBlank { "UntitledMolecule" },
                    "formula" to doc.formula.trim().take(300).ifBlank { "Prepare -> Combine -> Validate -> Seal" },
                    "model" to doc.model.trim().take(100).ifBlank { "KickLang v4.1 Molecule" },
                    "space" to doc.space.trim().take(120).ifBlank { "Kick/Space" },
                    "scope" to doc.scope.trim().take(300).ifBlank { "Domain molecule execution" },
                    "reference" to doc.reference.trim().take(300).ifBlank { "Flow-Playbook v1.1" },
                    "sourceCmdMode" to doc.sourceCmdMode,
                    "projectedRunFlagMode" to doc.projectedRunFlagMode,
                    "currentStage" to doc.currentStage,
                    "stageOutputs" to doc.stageOutputs.take(6),
                    "sonicDelegated" to doc.sonicDelegated,
                    "status" to doc.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.update(updates).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Sonic Runs ---

    suspend fun saveSonicRun(existingId: String?, doc: SonicRunDoc): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sonicRuns"
        return try {
            val uid = requireUserId()
            val col = userRoot(uid).collection("sonicRuns")
            if (existingId.isNullOrBlank()) {
                val ref = col.document()
                docPath = ref.path
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "title" to doc.title.trim().take(120).ifBlank { "Lyria3 Sonic Brief" },
                    "creativeBrief" to doc.creativeBrief.trim().take(1500).ifBlank { "Obsidian spatial texture" },
                    "energy" to doc.energy.coerceIn(1, 5),
                    "adaptiveTempo" to doc.adaptiveTempo,
                    "contrastPath" to doc.contrastPath,
                    "microTiming" to doc.microTiming.trim().take(120).ifBlank { "humanized +4ms" },
                    "spatialMapping" to doc.spatialMapping.trim().take(300).ifBlank { "wide obsidian hall" },
                    "consentRequired" to doc.consentRequired,
                    "consentTokenPresent" to doc.consentTokenPresent,
                    "consentToken" to doc.consentToken?.take(128),
                    "kickGuardDimaApproved" to doc.kickGuardDimaApproved,
                    "kickGuardFizzApproved" to doc.kickGuardFizzApproved,
                    "kickGuardMonitorApproved" to doc.kickGuardMonitorApproved,
                    "promptEnvelope" to doc.promptEnvelope.take(20000),
                    "sonicCoherence" to doc.sonicCoherence?.coerceIn(0.0, 1.0),
                    "sonicStatus" to doc.sonicStatus,
                    "status" to doc.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(existingId)
                docPath = ref.path
                val updates = mutableMapOf<String, Any?>(
                    "title" to doc.title.trim().take(120).ifBlank { "Lyria3 Sonic Brief" },
                    "creativeBrief" to doc.creativeBrief.trim().take(1500).ifBlank { "Obsidian spatial texture" },
                    "energy" to doc.energy.coerceIn(1, 5),
                    "adaptiveTempo" to doc.adaptiveTempo,
                    "contrastPath" to doc.contrastPath,
                    "microTiming" to doc.microTiming.trim().take(120).ifBlank { "humanized +4ms" },
                    "spatialMapping" to doc.spatialMapping.trim().take(300).ifBlank { "wide obsidian hall" },
                    "consentRequired" to doc.consentRequired,
                    "consentTokenPresent" to doc.consentTokenPresent,
                    "consentToken" to doc.consentToken?.take(128),
                    "kickGuardDimaApproved" to doc.kickGuardDimaApproved,
                    "kickGuardFizzApproved" to doc.kickGuardFizzApproved,
                    "kickGuardMonitorApproved" to doc.kickGuardMonitorApproved,
                    "promptEnvelope" to doc.promptEnvelope.take(20000),
                    "sonicCoherence" to doc.sonicCoherence?.coerceIn(0.0, 1.0),
                    "sonicStatus" to doc.sonicStatus,
                    "status" to doc.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.update(updates).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- Open Decisions & Conflicts ---

    suspend fun upsertDecision(
        decisionId: String,
        code: String,
        category: String,
        status: String,
        selectedResolution: String,
        rationale: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/decisions/$decisionId"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("decisions").document(decisionId)
            docPath = ref.path
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "status" to status,
                        "selectedResolution" to selectedResolution.trim().take(500),
                        "rationale" to rationale.trim().take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "code" to code.take(20),
                        "category" to category,
                        "status" to status,
                        "selectedResolution" to selectedResolution.trim().take(500),
                        "rationale" to rationale.trim().take(1500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- RULE-11 Immutable Sealed Siblings ---

    suspend fun mintSealedSibling(
        versionLabel: String,
        parentVersionRef: String,
        sourceKind: String,
        sourceId: String,
        structuralCoherence: Double,
        sonicCoherence: Double?,
        sonicStatus: String,
        transcript: String
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sealedSiblings"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("sealedSiblings").document()
            docPath = ref.path
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "versionLabel" to versionLabel.trim().take(120).ifBlank { "OCS Sibling v1.0.x" },
                "parentVersionRef" to parentVersionRef.trim().take(160).ifBlank { "OCS KickLang Super Playbook v1.0.0" },
                "protocolVersion" to "2.2",
                "languageModel" to "KickLang v4.1 Molecule",
                "sourceKind" to sourceKind,
                "sourceId" to sourceId.take(128).ifBlank { "session" },
                "structuralCoherence" to structuralCoherence.coerceIn(0.0, 1.0),
                "sonicCoherence" to sonicCoherence?.coerceIn(0.0, 1.0),
                "sonicStatus" to sonicStatus,
                "transcript" to transcript.take(40000).ifBlank { "⫻end/flow" },
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
