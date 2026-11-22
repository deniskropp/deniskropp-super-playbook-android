package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MoleculeRunDoc
import com.example.data.SonicRunDoc
import com.example.domain.CmdMode
import com.example.domain.OcsRuleValidator
import com.example.domain.ProtocolRegistry
import com.example.domain.RunFlagMode
import com.example.domain.SonicContrastPath
import com.example.ui.components.DisplayHeaderCard
import com.example.ui.components.ErrorTelemetryBanner
import com.example.ui.components.KickLangCodeViewer
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.LocalOcsColors
import com.example.ui.viewmodel.OcsRuntimeViewModel
import com.example.ui.viewmodel.RuntimeActionState
import com.example.ui.viewmodel.StreamState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EnginesScreen(
    moleculesState: StreamState<List<MoleculeRunDoc>>,
    sonicRunsState: StreamState<List<SonicRunDoc>>,
    actionState: RuntimeActionState,
    viewModel: OcsRuntimeViewModel,
    onShowMessage: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Molecule, 1 = Sonic
    val ocs = LocalOcsColors.current
    val molecules = (moleculesState as? StreamState.Success)?.data.orEmpty()
    val sonicRuns = (sonicRunsState as? StreamState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("engines_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DisplayHeaderCard(
                executable = if (selectedTab == 0) "⫻flow/molecule · Compact Runtime (PTAS-06)" else "⫻flow/qllickfizz_sonic · Lyria3 Domain Engine",
                input = if (selectedTab == 0) "⫻context/klmx Hexad + run_flags.mode projection" else "Sonic Params + RULE-10 KickGuard Consent + Wet-Lift Axis",
                expectedOutput = if (selectedTab == 0) "Stage transcript (Prepare..Seal) + RULE-11 Seal" else "FORMAT-09 ⫻prompt/* Envelope + Sonic Axis (0.93)"
            )
        }

        if (actionState.errorMessage != null) {
            item {
                ErrorTelemetryBanner(message = actionState.errorMessage, onDismiss = { viewModel.clearError() })
            }
        }

        if (actionState.busyLabel != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ocs.sonicVioletDim,
                    border = BorderStroke(1.dp, ocs.sonicViolet)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ocs.sonicViolet, strokeWidth = 2.5.dp)
                        Text(actionState.busyLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("molecule_engine_tab")
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("⫻flow/molecule")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("sonic_engine_tab")
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("⫻flow/sonic")
                }
            }
        }

        if (selectedTab == 0) {
            item {
                MoleculeEnginePanel(
                    molecules = molecules,
                    isBusy = actionState.busyLabel != null,
                    viewModel = viewModel,
                    onCopied = { onShowMessage("Molecule KickLang transcript copied.") }
                )
            }
        } else {
            item {
                SonicEnginePanel(
                    sonicRuns = sonicRuns,
                    isBusy = actionState.busyLabel != null,
                    viewModel = viewModel,
                    onCopied = { onShowMessage("FORMAT-09 Lyria3 envelope copied.") }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoleculeEnginePanel(
    molecules: List<MoleculeRunDoc>,
    isBusy: Boolean,
    viewModel: OcsRuntimeViewModel,
    onCopied: () -> Unit
) {
    val ocs = LocalOcsColors.current
    val latest = molecules.firstOrNull()
    var activeId by remember(latest?.id) { mutableStateOf(latest?.id) }
    var molecule by remember(latest?.id) { mutableStateOf(latest?.molecule ?: "CognitivePulse") }
    var formula by remember(latest?.id) { mutableStateOf(latest?.formula ?: "Prepare + Combine -> Validate -> Finalize -> Seal") }
    var space by remember(latest?.id) { mutableStateOf(latest?.space ?: "Kick/Space") }
    var scope by remember(latest?.id) { mutableStateOf(latest?.scope ?: "Compact molecule execution without 20-step expansion") }
    var reference by remember(latest?.id) { mutableStateOf(latest?.reference ?: "Flow-Playbook v1.1 · module<FlowPlaybookMolecules>") }
    var sourceMode by remember(latest?.id) { mutableStateOf(CmdMode.fromNameSafe(latest?.sourceCmdMode ?: "Predictive")) }
    var projectedMode by remember(latest?.id) { mutableStateOf(RunFlagMode.fromNameSafe(latest?.projectedRunFlagMode ?: "Swarm")) }
    var sonicDelegated by remember(latest?.id) { mutableStateOf(latest?.sonicDelegated ?: false) }

    val stageOutputs = latest?.stageOutputs.orEmpty()
    val nextStageIdx = stageOutputs.size.coerceAtMost(ProtocolRegistry.moleculeStages.lastIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ocs.kickCyan.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⫻context/klmx HEXAD", style = MaterialTheme.typography.labelLarge, color = ocs.kickCyan)
                    OutlinedButton(
                        onClick = {
                            activeId = null
                            molecule = "NewMolecule"
                            formula = "Prepare -> Combine -> Validate -> Finalize -> Seal"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("New Molecule", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(value = molecule, onValueChange = { molecule = it }, label = { Text("molecule") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = formula, onValueChange = { formula = it }, label = { Text("formula") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = space, onValueChange = { space = it }, label = { Text("space") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ProtocolRegistry.LANGUAGE_MODEL, onValueChange = {}, readOnly = true, label = { Text("model") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = scope, onValueChange = { scope = it }, label = { Text("scope") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("reference") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Mode Projection: ⫻cmd/mode → run_flags.mode (Step 8)", style = MaterialTheme.typography.labelMedium, color = ocs.haltAmber)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CmdMode.entries.forEach { m ->
                        FilterChip(
                            selected = sourceMode == m,
                            onClick = {
                                sourceMode = m
                                projectedMode = m.projectToRunFlagMode(projectedMode)
                            },
                            label = { Text(m.label) }
                        )
                    }
                }
                TelemetryPill("Projected run_flags.mode = ${projectedMode.label}", ocs.structuralEmerald)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Delegate Sonic Synthesis", style = MaterialTheme.typography.titleSmall)
                        Text("Hand off domain brief to ⫻flow/qllickfizz_sonic (PTAS-06)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = sonicDelegated, onCheckedChange = { sonicDelegated = it })
                }
            }
        }

        // Compact 6-Stage Pipeline
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ocs.hairline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("COMPACT PIPELINE (Prepare → Seal)", style = MaterialTheme.typography.labelLarge, color = ocs.kickCyan)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProtocolRegistry.moleculeStages.forEachIndexed { idx, stage ->
                        val done = idx < stageOutputs.size
                        TelemetryPill(
                            text = "${idx + 1}. $stage",
                            accent = when {
                                done -> ocs.structuralEmerald
                                idx == nextStageIdx -> ocs.kickCyan
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                val isSealed = latest?.status == "SEALED" && activeId == latest.id
                Button(
                    onClick = {
                        viewModel.runMoleculeStage(
                            existingId = activeId,
                            molecule = molecule,
                            formula = formula,
                            space = space,
                            scope = scope,
                            reference = reference,
                            sourceCmdMode = sourceMode,
                            projectedRunFlag = projectedMode,
                            sonicDelegated = sonicDelegated,
                            stageIndex = if (activeId == null) 0 else nextStageIdx,
                            priorOutputs = if (activeId == null) emptyList() else stageOutputs
                        )
                    },
                    enabled = !isBusy && !isSealed,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_molecule_stage_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = when {
                            isSealed -> "Molecule Sealed (RULE-11)"
                            activeId == null -> "Run Stage 1 · Prepare"
                            else -> "Run Stage ${nextStageIdx + 1} · ${ProtocolRegistry.moleculeStages[nextStageIdx]}"
                        }
                    )
                }

                if (stageOutputs.isNotEmpty()) {
                    KickLangCodeViewer(
                        code = stageOutputs.joinToString("\n\n"),
                        title = "⫻flow/molecule · ${latest?.molecule ?: molecule}",
                        onCopied = onCopied
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SonicEnginePanel(
    sonicRuns: List<SonicRunDoc>,
    isBusy: Boolean,
    viewModel: OcsRuntimeViewModel,
    onCopied: () -> Unit
) {
    val ocs = LocalOcsColors.current
    val latest = sonicRuns.firstOrNull()
    var activeId by remember(latest?.id) { mutableStateOf(latest?.id) }
    var title by remember(latest?.id) { mutableStateOf(latest?.title ?: "Obsidian Full Bloom") }
    var brief by remember(latest?.id) { mutableStateOf(latest?.creativeBrief ?: "Cinematic analog synth arpeggio evolving through obsidian resonance into luminous polyrhythmic bloom.") }
    var energy by remember(latest?.id) { mutableFloatStateOf((latest?.energy ?: 4).toFloat()) }
    var adaptiveTempo by remember(latest?.id) { mutableStateOf(latest?.adaptiveTempo ?: true) }
    var contrastPath by remember(latest?.id) { mutableStateOf(SonicContrastPath.fromCodeSafe(latest?.contrastPath ?: "PATH_D_FULL_BLOOM")) }
    var microTiming by remember(latest?.id) { mutableStateOf(latest?.microTiming ?: "humanized +6ms groove offset") }
    var spatialMapping by remember(latest?.id) { mutableStateOf(latest?.spatialMapping ?: "wide obsidian dome · 270deg stereo halo") }

    var dimaOk by remember(latest?.id) { mutableStateOf(latest?.kickGuardDimaApproved ?: false) }
    var fizzOk by remember(latest?.id) { mutableStateOf(latest?.kickGuardFizzApproved ?: false) }
    var monitorOk by remember(latest?.id) { mutableStateOf(latest?.kickGuardMonitorApproved ?: false) }

    val energyInt = energy.roundToInt().coerceIn(1, 5)
    val consentRequired = OcsRuleValidator.requiresSonicConsentGate(energyInt, adaptiveTempo, contrastPath)
    val consentReady = !consentRequired || (dimaOk && fizzOk && monitorOk)
    val isSealed = latest?.status == "SEALED" && activeId == latest.id

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ocs.sonicViolet.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LYRIA3 DOMAIN PARAMETERS", style = MaterialTheme.typography.labelLarge, color = ocs.sonicViolet)
                    OutlinedButton(
                        onClick = {
                            activeId = null
                            dimaOk = false; fizzOk = false; monitorOk = false
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("New Brief", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Sonic Brief Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brief, onValueChange = { brief = it }, label = { Text("Creative Direction") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Energy Level", style = MaterialTheme.typography.labelLarge)
                        TelemetryPill(
                            text = "$energyInt / 5" + if (energyInt >= 4) " · RULE-10" else "",
                            accent = if (energyInt >= 4) ocs.haltAmber else ocs.kickCyan
                        )
                    }
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Adaptive Tempo", style = MaterialTheme.typography.titleSmall)
                        Text("Triggers RULE-10 consent gate when active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = adaptiveTempo, onCheckedChange = { adaptiveTempo = it })
                }

                Text("Contrast Path", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SonicContrastPath.entries.forEach { path ->
                        FilterChip(
                            selected = contrastPath == path,
                            onClick = { contrastPath = path },
                            label = { Text(path.label) }
                        )
                    }
                }

                OutlinedTextField(value = microTiming, onValueChange = { microTiming = it }, label = { Text("micro_timing (Fizz rule)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = spatialMapping, onValueChange = { spatialMapping = it }, label = { Text("Sonic Spatial Mapping (⫻context/)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        // RULE-10 Consent Gate Panel
        if (consentRequired) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ocs.haltAmberDim,
                border = BorderStroke(1.5.dp, ocs.haltAmber)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = "RULE-10 Consent Gate", tint = ocs.haltAmber)
                        Text("RULE-10 ⫻cmd/halt · KickGuard Consent Required", style = MaterialTheme.typography.titleSmall, color = ocs.haltAmber)
                    }
                    Text(
                        text = "Triggered by Energy >= 4, adaptive tempo, or Path D full_bloom. Confirm KickGuard triad to mint consent_token:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ConsentCheckRow("Dima · Ethical / Joint Gate Confirmed", dimaOk) { dimaOk = it }
                    ConsentCheckRow("QllickFizz · Micro-timing & FORMAT-09 Compliance", fizzOk) { fizzOk = it }
                    ConsentCheckRow("SystemMonitor · Dual-Axis Metric Integrity Ready", monitorOk) { monitorOk = it }
                    if (latest?.consentTokenPresent == true && activeId == latest.id) {
                        TelemetryPill("consent_token: ${latest.consentToken}", ocs.structuralEmerald)
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.generateSonicBrief(
                    existingId = activeId,
                    title = title,
                    creativeBrief = brief,
                    energy = energyInt,
                    adaptiveTempo = adaptiveTempo,
                    contrastPath = contrastPath,
                    microTiming = microTiming,
                    spatialMapping = spatialMapping,
                    dimaApproved = dimaOk,
                    fizzApproved = fizzOk,
                    monitorApproved = monitorOk
                )
            },
            enabled = !isBusy && !isSealed,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_sonic_brief_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (consentReady) ocs.sonicViolet else ocs.haltAmber,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Icon(
                imageVector = if (consentReady) Icons.Default.GraphicEq else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    isSealed -> "Sonic Run Sealed (RULE-11)"
                    !consentReady -> "Halted · Complete RULE-10 KickGuard Consent"
                    else -> "Generate FORMAT-09 Lyria3 Envelope"
                }
            )
        }

        // Wet-Lift Scoring Card (RULE-09: Sonic stays OPEN until wet-lift!)
        if (latest != null && latest.promptEnvelope.isNotBlank() && activeId == latest.id) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ocs.sonicViolet.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SONIC AXIS COHERENCE (RULE-09)", style = MaterialTheme.typography.labelMedium, color = ocs.sonicViolet)
                            Text(
                                text = latest.sonicCoherence?.let { String.format(Locale.US, "Measured: %.3f (Target 0.93)", it) }
                                    ?: "OPEN · pending_wet_lift (never placeholder-scored)",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        TelemetryPill(latest.sonicStatus.uppercase(), if (latest.sonicStatus == "scored") ocs.structuralEmerald else ocs.sonicViolet)
                    }
                    OutlinedButton(
                        onClick = { viewModel.runSonicWetLiftScoring(latest) },
                        enabled = !isBusy && latest.status != "SEALED",
                        modifier = Modifier.fillMaxWidth().testTag("wet_lift_score_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp), tint = ocs.sonicViolet)
                        Spacer(Modifier.width(6.dp))
                        Text(if (latest.status == "SEALED") "Wet-Lift Passed & Sealed" else "Run Wet-Lift Scoring Pass (Target >= 0.93)")
                    }
                    KickLangCodeViewer(
                        code = latest.promptEnvelope,
                        title = "FORMAT-09 ⫻prompt/* · ${latest.title}",
                        onCopied = onCopied
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
