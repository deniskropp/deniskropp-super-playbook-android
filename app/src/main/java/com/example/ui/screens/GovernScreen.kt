package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DecisionRecordDoc
import com.example.data.SealedSiblingDoc
import com.example.domain.OpenDecisionSpec
import com.example.domain.ProtocolRegistry
import com.example.ui.components.DisplayHeaderCard
import com.example.ui.components.KickLangCodeViewer
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.LocalOcsColors
import com.example.ui.viewmodel.OcsRuntimeViewModel
import com.example.ui.viewmodel.StreamState
import java.util.Locale

@Composable
fun GovernScreen(
    decisionsState: StreamState<List<DecisionRecordDoc>>,
    sealedSiblingsState: StreamState<List<SealedSiblingDoc>>,
    viewModel: OcsRuntimeViewModel,
    onShowMessage: (String) -> Unit
) {
    val ocs = LocalOcsColors.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0=Open 1-12, 1=Conflicts SP-A..H, 2=Rules, 3=Seals
    var editingDecision by remember { mutableStateOf<OpenDecisionSpec?>(null) }

    val savedDecisions = ((decisionsState as? StreamState.Success)?.data.orEmpty()).associateBy { it.id }
    val sealedSiblings = (sealedSiblingsState as? StreamState.Success)?.data.orEmpty()
    val resolvedCount = savedDecisions.values.count { it.status == "CONFIRMED" || it.status == "AMENDED" }

    val sectionLabels = listOf(
        "Open 1–12",
        "Conflicts SP-A..H",
        "Rules (21)",
        "Seals (${sealedSiblings.size})"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("govern_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DisplayHeaderCard(
                executable = "Denis Governance Console · Open Decisions & RULE-11 Ledger",
                input = "12 Inherited Decisions + 8 Derivation Conflicts ($resolvedCount/20 resolved)",
                expectedOutput = "Cloud-synced ⫻data/conflict resolutions & immutable RULE-11 sibling lineage"
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sectionLabels.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("govern_tab_$idx")
                    )
                }
            }
        }

        when (selectedSection) {
            0 -> {
                val inherited = ProtocolRegistry.openDecisions.filter { it.category == "INHERITED" }
                items(inherited, key = { it.id }) { spec ->
                    DecisionCard(
                        spec = spec,
                        saved = savedDecisions[spec.id],
                        onClick = { editingDecision = spec }
                    )
                }
            }
            1 -> {
                val conflicts = ProtocolRegistry.openDecisions.filter { it.category == "CONFLICT" }
                items(conflicts, key = { it.id }) { spec ->
                    DecisionCard(
                        spec = spec,
                        saved = savedDecisions[spec.id],
                        onClick = { editingDecision = spec }
                    )
                }
            }
            2 -> {
                items(ProtocolRegistry.rules, key = { it.code }) { rule ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ocs.hairline)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rule.code, style = MaterialTheme.typography.labelLarge, color = if (rule.isFormat) ocs.sonicViolet else ocs.kickCyan)
                                TelemetryPill(rule.owner, if (rule.isFormat) ocs.sonicViolet else ocs.haltAmber)
                            }
                            Text(rule.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            else -> {
                if (sealedSiblings.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ocs.hairline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No Sealed Siblings Yet (RULE-11)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Complete Step 20 atomic 14-agent sign-off in Cockpit, Seal a Molecule, or pass Wet-Lift (>= 0.93) in Sonique to mint an immutable RULE-11 sibling.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(sealedSiblings, key = { it.id }) { seal ->
                        SealedSiblingCard(
                            seal = seal,
                            onCopied = { onShowMessage("Sealed sibling transcript copied.") }
                        )
                    }
                }
            }
        }
    }

    val target = editingDecision
    if (target != null) {
        DecisionEditDialog(
            spec = target,
            saved = savedDecisions[target.id],
            onSave = { status, resolution, rationale ->
                viewModel.saveDecisionResolution(target, status, resolution, rationale)
                editingDecision = null
            },
            onDismiss = { editingDecision = null }
        )
    }
}

@Composable
private fun DecisionCard(
    spec: OpenDecisionSpec,
    saved: DecisionRecordDoc?,
    onClick: () -> Unit
) {
    val ocs = LocalOcsColors.current
    val status = saved?.status ?: "OPEN"
    val accent = when (status) {
        "CONFIRMED" -> ocs.structuralEmerald
        "AMENDED" -> ocs.kickCyan
        "DEFERRED" -> ocs.sonicViolet
        else -> ocs.haltAmber
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${spec.code} · ${spec.title}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                TelemetryPill(status, accent)
            }
            Text(spec.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val chosen = saved?.selectedResolution?.takeIf { it.isNotBlank() } ?: spec.defaultDraft
            Text("Current position: $chosen", style = MaterialTheme.typography.labelSmall, color = ocs.kickCyan)
        }
    }
}

@Composable
private fun DecisionEditDialog(
    spec: OpenDecisionSpec,
    saved: DecisionRecordDoc?,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var status by remember { mutableStateOf(saved?.status ?: "CONFIRMED") }
    var selectedOpt by remember { mutableStateOf(saved?.selectedResolution?.ifBlank { spec.defaultDraft } ?: spec.defaultDraft) }
    var rationale by remember { mutableStateOf(saved?.rationale ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${spec.code} · ${spec.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(spec.summary, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("OPEN", "CONFIRMED", "AMENDED", "DEFERRED").forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                    }
                }
                spec.options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOpt = opt },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedOpt == opt, onClick = { selectedOpt = opt })
                        Text(opt, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = rationale,
                    onValueChange = { rationale = it },
                    label = { Text("Denis Seal Rationale / Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(status, selectedOpt, rationale) }) {
                Text("Save Resolution")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SealedSiblingCard(
    seal: SealedSiblingDoc,
    onCopied: () -> Unit
) {
    val ocs = LocalOcsColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ocs.structuralEmerald.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = "Immutable Seal", tint = ocs.structuralEmerald, modifier = Modifier.size(16.dp))
                    Text(seal.versionLabel, style = MaterialTheme.typography.titleSmall)
                }
                TelemetryPill("IMMUTABLE", ocs.structuralEmerald)
            }
            Text("Parent cited (RULE-11): ${seal.parentVersionRef}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryPill("Proto v${seal.protocolVersion}", ocs.kickCyan)
                TelemetryPill("Struct ${String.format(Locale.US, "%.3f", seal.structuralCoherence)}", ocs.structuralEmerald)
                TelemetryPill(
                    text = seal.sonicCoherence?.let { String.format(Locale.US, "Sonic %.3f", it) } ?: "Sonic ${seal.sonicStatus}",
                    accent = ocs.sonicViolet
                )
            }
            Spacer(Modifier.height(2.dp))
            KickLangCodeViewer(
                code = seal.transcript,
                title = "Sealed Transcript · ${seal.sourceKind}",
                onCopied = onCopied
            )
        }
    }
}
