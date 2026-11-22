package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlaybookRunDoc
import com.example.data.StepExecutionDoc
import com.example.domain.CmdLang
import com.example.domain.CmdMode
import com.example.domain.MetaStepBlueprint
import com.example.domain.ProtocolRegistry
import com.example.domain.RunFlagMode
import com.example.ui.components.DisplayFooterCard
import com.example.ui.components.DisplayHeaderCard
import com.example.ui.components.DualAxisCoherenceStrip
import com.example.ui.components.ErrorTelemetryBanner
import com.example.ui.components.KickLangCodeViewer
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.LocalOcsColors
import com.example.ui.viewmodel.OcsRuntimeViewModel
import com.example.ui.viewmodel.RuntimeActionState
import com.example.ui.viewmodel.StreamState

@Composable
fun CockpitScreen(
    runsState: StreamState<List<PlaybookRunDoc>>,
    stepsState: StreamState<List<StepExecutionDoc>>,
    selectedRunId: String?,
    actionState: RuntimeActionState,
    viewModel: OcsRuntimeViewModel,
    onShowMessage: (String) -> Unit
) {
    val ocs = LocalOcsColors.current
    var showNewRunDialog by remember { mutableStateOf(false) }
    var expandedStepNumber by remember { mutableStateOf<Int?>(null) }

    val runs = (runsState as? StreamState.Success)?.data.orEmpty()
    val steps = (stepsState as? StreamState.Success)?.data.orEmpty()
    val activeRun = runs.firstOrNull { it.id == selectedRunId } ?: runs.firstOrNull()

    LaunchedEffect(runs, selectedRunId) {
        if (selectedRunId == null && runs.isNotEmpty()) {
            viewModel.selectRun(runs.first().id)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cockpit_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // FORMAT-08 Mandatory Display Header
        item {
            DisplayHeaderCard(
                executable = activeRun?.let { "⫻flow/meta-playbook · Step ${it.stepsDone}/20 (${it.cmdMode})" }
                    ?: "⫻flow/meta-playbook · Initialize Objective",
                input = activeRun?.objective ?: "Select or create a ⫻data/obj run to invoke the 14-agent swarm",
                expectedOutput = "Validated KickLang ⫻ sections · Dual-axis ⫻data/var · RULE-11 Sealed Sibling"
            )
        }

        if (actionState.errorMessage != null) {
            item {
                ErrorTelemetryBanner(
                    message = actionState.errorMessage,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }

        if (actionState.busyLabel != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ocs.kickCyanDim,
                    border = BorderStroke(1.dp, ocs.kickCyan)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ocs.kickCyan,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = actionState.busyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Run Switcher + New Run Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ACTIVE PLAYBOOK RUNS (${runs.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showNewRunDialog = true },
                    modifier = Modifier.testTag("new_run_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New run", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New ⫻data/obj", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (runs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    runs.forEach { run ->
                        val selected = run.id == activeRun?.id
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectRun(run.id) },
                            label = {
                                Text(
                                    text = "${run.title.take(22)} · ${run.stepsDone}/20",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = if (run.status == "SEALED") {
                                { Icon(Icons.Default.Lock, contentDescription = "Sealed", modifier = Modifier.size(15.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        if (runsState is StreamState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ocs.kickCyan)
                }
            }
        } else if (runsState is StreamState.Error) {
            item {
                ErrorTelemetryBanner(message = runsState.message, onDismiss = {})
            }
        } else if (activeRun == null) {
            item {
                ZeroStateCockpitCard(
                    onStartPreset = { title, obj, mode ->
                        viewModel.createPlaybookRun(
                            title = title,
                            objective = obj,
                            cmdMode = mode,
                            hybridComponents = null,
                            projectedRunFlagMode = mode.projectToRunFlagMode(),
                            cmdLang = CmdLang.EN
                        )
                    },
                    onOpenCustom = { showNewRunDialog = true }
                )
            }
        } else {
            // Live Dual-Axis Telemetry Strip
            item {
                DualAxisCoherenceStrip(
                    structuralValue = activeRun.structuralCoherence,
                    structuralStatus = activeRun.structuralStatus,
                    sonicValue = activeRun.sonicCoherence,
                    sonicStatus = activeRun.sonicStatus,
                    invokedCount = activeRun.invokedAtomicAgents.size,
                    stepsDone = activeRun.stepsDone,
                    driftCount = activeRun.schemaDriftTypes.size
                )
            }

            // Primary Execution Controls
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ocs.hairline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activeRun.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Mode: ${activeRun.cmdMode} → run_flags.${activeRun.projectedRunFlagMode} · Lang: ${activeRun.cmdLang}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ocs.kickCyan
                                )
                            }
                            if (activeRun.status != "SEALED") {
                                IconButton(onClick = { viewModel.deleteRun(activeRun.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete run",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                TelemetryPill("RULE-11 SEALED", ocs.structuralEmerald)
                            }
                        }

                        if (!activeRun.haltReason.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ocs.haltAmberDim,
                                border = BorderStroke(1.dp, ocs.haltAmber.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = "Halt", tint = ocs.haltAmber, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = activeRun.haltReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val busy = actionState.busyLabel != null
                            val isSealed = activeRun.status == "SEALED"
                            Button(
                                onClick = { viewModel.executeNextStep(activeRun, steps) },
                                enabled = !busy && !isSealed && activeRun.stepsDone < 20,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("execute_next_step_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (activeRun.stepsDone < 20) "Step ${activeRun.stepsDone + 1}" else "All 20 Done",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.runPhaseToNextGate(activeRun, steps) },
                                enabled = !busy && !isSealed && activeRun.stepsDone < 20,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("auto_run_to_gate_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ocs.kickCyan.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Run → Gate", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // Gate Quick-Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeRun.stepsDone >= 10 && activeRun.status == "HALTED") {
                                OutlinedButton(onClick = { viewModel.openGate("DIMA_JOINT_DECISION") }) {
                                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp), tint = ocs.haltAmber)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Open Dima Gate", color = ocs.haltAmber)
                                }
                            }
                            if (activeRun.stepsDone >= 13) {
                                OutlinedButton(onClick = { viewModel.openGate("WEPLAN_SELECT") }) {
                                    Text("WePlan OPT-A..E (${activeRun.selectedWePlanOptions.size})")
                                }
                            }
                            if (activeRun.stepsDone >= 19 || activeRun.status == "SEALED") {
                                Button(
                                    onClick = { viewModel.openGate("STEP20_SIGNOFF") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ocs.structuralEmerald,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Step 20 Sign-Off (${activeRun.atomicSignOffCount}/14)")
                                }
                            }
                        }
                    }
                }
            }

            // 20-Step Execution Framework List
            items(ProtocolRegistry.steps, key = { it.stepNumber }) { blueprint ->
                val execDoc = steps.firstOrNull { it.stepNumber == blueprint.stepNumber }
                val isExpanded = expandedStepNumber == blueprint.stepNumber
                StepTimelineCard(
                    blueprint = blueprint,
                    execDoc = execDoc,
                    isNext = blueprint.stepNumber == activeRun.stepsDone + 1 && activeRun.status != "SEALED",
                    isExpanded = isExpanded,
                    isSealed = activeRun.status == "SEALED",
                    isBusy = actionState.busyLabel != null,
                    onToggleExpand = {
                        expandedStepNumber = if (isExpanded) null else blueprint.stepNumber
                    },
                    onRunStep = { viewModel.executeSpecificStep(activeRun, steps, blueprint.stepNumber) },
                    onCopied = { onShowMessage("KickLang output copied to clipboard.") }
                )
            }

            item {
                DisplayFooterCard(
                    contextNote = "${ProtocolRegistry.DOCUMENT_VERSION} · ⫻protocol/ocs v${ProtocolRegistry.PROTOCOL_VERSION}",
                    summary = "Completed ${activeRun.stepsDone}/20 steps · ${activeRun.invokedAtomicAgents.size}/14 atomic agents · Structural ${activeRun.structuralCoherence?.let { String.format("%.3f", it) } ?: "pending"} · Sonic ${activeRun.sonicStatus}.",
                    nextAction = when {
                        activeRun.status == "SEALED" -> "Sealed under RULE-11. Inspect in Govern → Sealed Siblings."
                        activeRun.stepsDone >= 20 -> "Complete 14-agent atomic sign-off & mint immutable RULE-11 sibling."
                        else -> "Execute Step ${activeRun.stepsDone + 1} (${ProtocolRegistry.steps[activeRun.stepsDone.coerceIn(0, 19)].agentName})."
                    }
                )
            }
        }
    }

    if (showNewRunDialog) {
        NewPlaybookRunDialog(
            onDismiss = { showNewRunDialog = false },
            onCreate = { title, obj, mode, hybrid, proj, lang ->
                showNewRunDialog = false
                viewModel.createPlaybookRun(title, obj, mode, hybrid, proj, lang)
            }
        )
    }

    if (activeRun != null && actionState.activeGateKind != null) {
        when (actionState.activeGateKind) {
            "DIMA_COMPLIANCE", "DIMA_JOINT_DECISION" -> {
                DimaGateDialog(
                    run = activeRun,
                    isJointDecision = actionState.activeGateKind == "DIMA_JOINT_DECISION",
                    onApprove = { note -> viewModel.approveDimaGate(activeRun, note) },
                    onDismiss = { viewModel.dismissGate() }
                )
            }
            "WEPLAN_SELECT" -> {
                WePlanOptionsDialog(
                    initialSelection = activeRun.selectedWePlanOptions,
                    onConfirm = { selected -> viewModel.saveWePlanSelection(activeRun, selected) },
                    onDismiss = { viewModel.dismissGate() }
                )
            }
            "STEP20_SIGNOFF" -> {
                Step20SignOffDialog(
                    run = activeRun,
                    steps = steps,
                    onToggleAgent = { agent -> viewModel.toggleAgentSignOff(activeRun, agent) },
                    onSignAll = { viewModel.signOffAll14Agents(activeRun) },
                    onSeal = { label -> viewModel.sealPlaybookRunAsSibling(activeRun, steps, label) },
                    onDismiss = { viewModel.dismissGate() }
                )
            }
        }
    }
}

@Composable
private fun ZeroStateCockpitCard(
    onStartPreset: (String, String, CmdMode) -> Unit,
    onOpenCustom: () -> Unit
) {
    val ocs = LocalOcsColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ocs.hairlineBright)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = ocs.kickCyan)
                Text("Start Your First Meta-Playbook Run", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Launch a live 14-agent Gemini swarm on any ⫻data/obj objective, or select a Super Playbook v1.0.0 quick starter:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    onStartPreset(
                        "TAS-FORECAST Predictive Audit",
                        "Evaluate TAS-FORECAST as a Predictive-mode alias for projecting multi-domain coherence and roadmap risks across 5 domains.",
                        CmdMode.Predictive
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("preset_tas_forecast_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Preset: TAS-FORECAST (Predictive Mode · SP-B)", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = {
                    onStartPreset(
                        "OPT-D Protocol v3.0 Readiness",
                        "Run a full measured 20-step validation of OCS KickLang Super Playbook v1.0.0 to assess OPT-D readiness for Denis seal.",
                        CmdMode.Swarm
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Preset: OPT-D Measured Readiness (Swarm Mode)", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onOpenCustom,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Enter Custom ⫻data/obj Objective")
            }
        }
    }
}

@Composable
private fun StepTimelineCard(
    blueprint: MetaStepBlueprint,
    execDoc: StepExecutionDoc?,
    isNext: Boolean,
    isExpanded: Boolean,
    isSealed: Boolean,
    isBusy: Boolean,
    onToggleExpand: () -> Unit,
    onRunStep: () -> Unit,
    onCopied: () -> Unit
) {
    val ocs = LocalOcsColors.current
    val statusColor = when {
        execDoc == null && isNext -> ocs.kickCyan
        execDoc == null -> MaterialTheme.colorScheme.onSurfaceVariant
        execDoc.ruleViolations.isNotEmpty() -> ocs.driftCrimson
        execDoc.status == "HALTED" -> ocs.haltAmber
        else -> ocs.structuralEmerald
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isNext) 1.5.dp else 1.dp,
            color = if (isNext) ocs.kickCyan else ocs.hairline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (execDoc != null && execDoc.status == "COMPLETED" && execDoc.ruleViolations.isEmpty()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = statusColor, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = "${blueprint.stepNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phase ${blueprint.phase} · ${blueprint.agentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Text(
                        text = "${blueprint.title} (${blueprint.primitive})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (blueprint.requiresUserGate) {
                    TelemetryPill("GATE", ocs.haltAmber)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle step details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (execDoc != null) {
                Text(
                    text = execDoc.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = blueprint.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryPill("In: ${blueprint.inputPayload}", ocs.kickCyan)
                        TelemetryPill("Out: ${blueprint.expectedOutput}", ocs.sonicViolet)
                    }
                    if (execDoc != null) {
                        if (execDoc.ruleViolations.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ocs.driftCrimsonDim,
                                border = BorderStroke(1.dp, ocs.driftCrimson)
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("RULE / FORMAT VIOLATIONS", style = MaterialTheme.typography.labelSmall, color = ocs.driftCrimson)
                                    execDoc.ruleViolations.forEach { v ->
                                        Text("• $v", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                        KickLangCodeViewer(
                            code = buildString {
                                appendLine("⫻display/header")
                                appendLine("Executable: ${execDoc.displayExecutable}")
                                appendLine("Input: ${execDoc.displayInput}")
                                appendLine("Expected output: ${execDoc.displayExpectedOutput}")
                                appendLine()
                                append(execDoc.kickLangOutput)
                            },
                            title = "Step ${blueprint.stepNumber} · ${blueprint.agentName} (Iter ${execDoc.iterations})",
                            onCopied = onCopied
                        )
                    }
                    if (!isSealed) {
                        OutlinedButton(
                            onClick = onRunStep,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (execDoc == null) "Execute Step ${blueprint.stepNumber}" else "Re-run Step ${blueprint.stepNumber}")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewPlaybookRunDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, CmdMode, String?, RunFlagMode, CmdLang) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(CmdMode.Swarm) }
    var hybridComponents by remember { mutableStateOf("Swarm+Predictive") }
    var projectedFlag by remember { mutableStateOf(RunFlagMode.Swarm) }
    var selectedLang by remember { mutableStateOf(CmdLang.EN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New ⫻flow/meta-playbook Run") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Run Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("run_title_input")
                )
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Objective (⫻data/obj)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("run_objective_input")
                )
                Text("⫻cmd/mode", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CmdMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                projectedFlag = mode.projectToRunFlagMode(projectedFlag)
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }
                if (selectedMode == CmdMode.Hybrid) {
                    OutlinedTextField(
                        value = hybridComponents,
                        onValueChange = { hybridComponents = it },
                        label = { Text("Hybrid Components (e.g. Swarm+Predictive)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (selectedMode == CmdMode.Predictive || selectedMode == CmdMode.Hybrid) {
                    Text("Step 8 run_flags.mode Projection (4-value subset)", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RunFlagMode.entries.forEach { flag ->
                            FilterChip(
                                selected = projectedFlag == flag,
                                onClick = { projectedFlag = flag },
                                label = { Text(flag.label) }
                            )
                        }
                    }
                }
                Text("⫻cmd/lang", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CmdLang.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title.ifBlank { "Meta-Playbook Run" },
                        objective,
                        selectedMode,
                        hybridComponents,
                        projectedFlag,
                        selectedLang
                    )
                },
                enabled = objective.isNotBlank(),
                modifier = Modifier.testTag("confirm_new_run_button")
            ) {
                Text("Initialize Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DimaGateDialog(
    run: PlaybookRunDoc,
    isJointDecision: Boolean,
    onApprove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isJointDecision) "Step 16 · Dima Joint Decision (RULE-06)" else "Step 10 · Dima Ethical Compliance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "⫻cmd/halt active. Verify no harmful/deceptive outputs, no unverified claims without ⫻data/cite, and no self-reported completion without SystemMonitor.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (run.schemaDriftTypes.isNotEmpty()) {
                    Text(
                        text = "Flagged Schema Drift: ${run.schemaDriftTypes.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalOcsColors.current.driftCrimson
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Denis / Dima Resolution Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApprove(note) }) { Text("Clear ⫻cmd/halt & Resume") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Halted") }
        }
    )
}

@Composable
private fun WePlanOptionsDialog(
    initialSelection: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WePlan Options (OPT-A .. OPT-E)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select one or multiple options for Step 18 TAS-ALIGN tailored planning:",
                    style = MaterialTheme.typography.bodySmall
                )
                ProtocolRegistry.wePlanOptions.forEach { opt ->
                    val checked = opt.code in selected
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (opt.hasSchemaDriftWarning && checked) LocalOcsColors.current.haltAmber
                            else LocalOcsColors.current.hairline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selected.remove(opt.code) else selected.add(opt.code)
                            }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) { if (!checked) selected.add(opt.code) } else selected.remove(opt.code)
                                }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${opt.code} · ${opt.target}", style = MaterialTheme.typography.titleSmall)
                                Text(opt.command, style = MaterialTheme.typography.labelSmall, color = LocalOcsColors.current.kickCyan)
                                Text(opt.statusNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty()
            ) {
                Text("Lock Selection (${selected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun Step20SignOffDialog(
    run: PlaybookRunDoc,
    steps: List<StepExecutionDoc>,
    onToggleAgent: (String) -> Unit,
    onSignAll: () -> Unit,
    onSeal: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var versionLabel by remember { mutableStateOf("OCS Super Playbook v1.0.1-${run.title.take(16).trim()}") }
    val ocs = LocalOcsColors.current
    val structural = run.structuralCoherence ?: 0.0
    val readyToSeal = run.atomicSignOffCount >= 14 && structural >= ProtocolRegistry.STRUCTURAL_FLOOR && run.status != "SEALED"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Step 20 · Atomic 14-Agent Sign-Off") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Atomic sign-off is required per agent (group review does not substitute). Structural >= 0.85 required; sonic remains ${run.sonicStatus}.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryPill("Signed ${run.atomicSignOffCount}/14", if (run.atomicSignOffCount == 14) ocs.structuralEmerald else ocs.haltAmber)
                    TelemetryPill("Structural ${String.format("%.3f", structural)}", if (structural >= 0.85) ocs.structuralEmerald else ocs.driftCrimson)
                }
                if (run.status != "SEALED") {
                    OutlinedButton(onClick = onSignAll, modifier = Modifier.fillMaxWidth()) {
                        Text("Record All 14 Atomic Sign-Offs + Measure")
                    }
                }
                ProtocolRegistry.agents.forEach { agent ->
                    val signed = agent.name in run.signedOffAgents
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = run.status != "SEALED") { onToggleAgent(agent.name) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = signed,
                            onCheckedChange = { if (run.status != "SEALED") onToggleAgent(agent.name) }
                        )
                        Column {
                            Text("${agent.name} (${agent.group.label})", style = MaterialTheme.typography.labelLarge)
                            Text(agent.signOffContract, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (run.status != "SEALED") {
                    OutlinedTextField(
                        value = versionLabel,
                        onValueChange = { versionLabel = it },
                        label = { Text("New Sibling Version Label (RULE-11)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSeal(versionLabel) },
                enabled = readyToSeal,
                colors = ButtonDefaults.buttonColors(containerColor = ocs.structuralEmerald, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (run.status == "SEALED") "Already Sealed" else "Freeze & Mint RULE-11 Seal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
