package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PlaybookRunDoc
import com.example.data.StepExecutionDoc
import com.example.domain.CoreGroup
import com.example.domain.ProtocolRegistry
import com.example.ui.components.DisplayHeaderCard
import com.example.ui.components.DualAxisCoherenceStrip
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.LocalOcsColors
import kotlin.math.cos
import kotlin.math.sin

private val visualSpecs = listOf(
    "VISUAL-01" to "TAS Dependency DAG (9 Primitives)",
    "VISUAL-02" to "20-Step × 14-Agent Swimlane",
    "VISUAL-03" to "14-Agent Swarm Topology Map",
    "VISUAL-04" to "Dual-Axis Coherence Dashboard",
    "VISUAL-05" to "Composite Group Alias Partition",
    "VISUAL-06" to "Three-Engine Flow Registry"
)

@Composable
fun VisualsScreen(
    activeRun: PlaybookRunDoc?,
    steps: List<StepExecutionDoc>
) {
    var selectedVisual by remember { mutableIntStateOf(0) }
    val ocs = LocalOcsColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("visuals_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DisplayHeaderCard(
                executable = "AR-00L · TAS-TRANSFORM (${visualSpecs[selectedVisual].first})",
                input = activeRun?.let { "Live telemetry from '${it.title}' (${it.stepsDone}/20 steps)" }
                    ?: "Authoritative OCS Super Playbook v1.0.0 schema",
                expectedOutput = "⫻data/visual:${visualSpecs[selectedVisual].first} — ${visualSpecs[selectedVisual].second}"
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visualSpecs.forEachIndexed { idx, (code, _) ->
                    FilterChip(
                        selected = selectedVisual == idx,
                        onClick = { selectedVisual = idx },
                        label = { Text(code) },
                        modifier = Modifier.testTag("visual_chip_${idx + 1}")
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ocs.kickCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${visualSpecs[selectedVisual].first} — ${visualSpecs[selectedVisual].second}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ocs.kickCyan
                    )
                    when (selectedVisual) {
                        0 -> Visual01TasDag(activeRun)
                        1 -> Visual02Swimlane(activeRun, steps)
                        2 -> Visual03SwarmMap(activeRun)
                        3 -> Visual04CoherenceDashboard(activeRun)
                        4 -> Visual05GroupPartition(activeRun)
                        else -> Visual06EngineRegistry()
                    }
                }
            }
        }
    }
}

@Composable
private fun Visual01TasDag(activeRun: PlaybookRunDoc?) {
    val ocs = LocalOcsColors.current
    val used = activeRun?.tasPrimitivesUsed?.toSet().orEmpty()
    val primitives = ProtocolRegistry.tasPrimitives
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val cols = 3
        val cellW = size.width / cols
        val cellH = size.height / 3f
        val centers = primitives.mapIndexed { idx, spec ->
            val col = idx % cols
            val row = idx / cols
            spec.name to Offset(cellW * col + cellW / 2f, cellH * row + cellH / 2f)
        }.toMap()

        // Draw directed edges
        primitives.forEach { spec ->
            val target = centers[spec.name] ?: return@forEach
            spec.dependsOn.forEach { dep ->
                val origin = centers[dep] ?: return@forEach
                drawLine(
                    color = ocs.kickCyan.copy(alpha = 0.45f),
                    start = origin,
                    end = target,
                    strokeWidth = 3f
                )
            }
        }

        // Draw 9 primitive nodes
        primitives.forEach { spec ->
            val center = centers[spec.name] ?: return@forEach
            val active = spec.name in used
            val ringColor = when {
                spec.alias != null -> ocs.haltAmber
                active -> ocs.structuralEmerald
                else -> ocs.kickCyan
            }
            drawCircle(color = ocs.consoleSurface, radius = 26f, center = center)
            drawCircle(color = ringColor, radius = 26f, center = center, style = Stroke(width = if (active) 5f else 3f))
            drawCircle(color = if (active) ringColor else inactiveColor.copy(alpha = 0.5f), radius = 7f, center = center)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        primitives.forEach { spec ->
            val active = spec.name in used
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = spec.name + (spec.alias?.let { " (alias $it)" } ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) ocs.structuralEmerald else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = spec.outputPayload,
                    style = MaterialTheme.typography.labelSmall,
                    color = ocs.kickCyan
                )
            }
        }
    }
}

@Composable
private fun Visual02Swimlane(activeRun: PlaybookRunDoc?, steps: List<StepExecutionDoc>) {
    val ocs = LocalOcsColors.current
    val completedNums = steps.map { it.stepNumber }.toSet()
    val haltedNums = steps.filter { it.status == "HALTED" }.map { it.stepNumber }.toSet()
    val agents = ProtocolRegistry.agents

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        val rows = agents.size
        val cols = 20
        val rowH = size.height / rows
        val colW = size.width / cols

        // Grid lanes
        for (r in 0..rows) {
            drawLine(ocs.hairline, Offset(0f, r * rowH), Offset(size.width, r * rowH), strokeWidth = 1f)
        }
        for (c in 0..cols) {
            drawLine(ocs.hairline.copy(alpha = 0.4f), Offset(c * colW, 0f), Offset(c * colW, size.height), strokeWidth = 1f)
        }

        // Plot each step cell on its atomic agent lanes
        ProtocolRegistry.steps.forEach { bp ->
            val c = bp.stepNumber - 1
            bp.atomicAgentsInvoked.forEach { agentName ->
                val r = agents.indexOfFirst { it.name == agentName }
                if (r >= 0) {
                    val cellColor = when {
                        bp.stepNumber in haltedNums -> ocs.haltAmber
                        bp.stepNumber in completedNums -> ocs.structuralEmerald
                        bp.requiresUserGate -> ocs.sonicViolet
                        else -> ocs.kickCyan.copy(alpha = 0.45f)
                    }
                    drawRoundRect(
                        color = cellColor,
                        topLeft = Offset(c * colW + 2f, r * rowH + 2f),
                        size = Size((colW - 4f).coerceAtLeast(3f), (rowH - 4f).coerceAtLeast(3f)),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TelemetryPill("Completed", ocs.structuralEmerald)
        TelemetryPill("Halted", ocs.haltAmber)
        TelemetryPill("Gate Step", ocs.sonicViolet)
        TelemetryPill("Pending", ocs.kickCyan)
    }
    Text(
        text = "14 atomic rows × 20 step columns · Active run steps done: ${activeRun?.stepsDone ?: 0}/20",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Visual03SwarmMap(activeRun: PlaybookRunDoc?) {
    val ocs = LocalOcsColors.current
    val invoked = activeRun?.invokedAtomicAgents?.toSet().orEmpty()
    val nonApex = ProtocolRegistry.agents.filter { it.group != CoreGroup.OrchestratorApex }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val apexCenter = Offset(size.width / 2f, size.height * 0.22f)
        val ringCenter = Offset(size.width / 2f, size.height * 0.56f)
        val radiusX = size.width * 0.38f
        val radiusY = size.height * 0.32f

        nonApex.forEachIndexed { idx, agent ->
            val angle = (2.0 * Math.PI * idx / nonApex.size) - Math.PI / 2.0
            val pos = Offset(
                x = ringCenter.x + (radiusX * cos(angle)).toFloat(),
                y = ringCenter.y + (radiusY * sin(angle)).toFloat()
            )
            val groupColor = when (agent.group) {
                CoreGroup.KickForge -> ocs.kickCyan
                CoreGroup.KickFlow -> ocs.sonicViolet
                CoreGroup.KickGuard -> ocs.haltAmber
                CoreGroup.OrchestratorApex -> ocs.structuralEmerald
            }
            val isInvoked = agent.name in invoked
            drawLine(groupColor.copy(alpha = if (isInvoked) 0.7f else 0.25f), apexCenter, pos, strokeWidth = if (isInvoked) 3f else 1.5f)
            drawCircle(color = ocs.consoleSurface, radius = 15f, center = pos)
            drawCircle(color = groupColor, radius = 15f, center = pos, style = Stroke(width = if (isInvoked) 4.5f else 2f))
            if (isInvoked) {
                drawCircle(color = ocs.structuralEmerald, radius = 5f, center = pos)
            }
        }

        // Orchestrator Apex above all three cores
        drawCircle(color = ocs.consoleSurface, radius = 24f, center = apexCenter)
        drawCircle(color = ocs.structuralEmerald, radius = 24f, center = apexCenter, style = Stroke(width = 5f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TelemetryPill("Apex: Orchestrator", ocs.structuralEmerald)
        TelemetryPill("KickForge (2)", ocs.kickCyan)
        TelemetryPill("KickFlow (4)", ocs.sonicViolet)
        TelemetryPill("KickGuard (7)", ocs.haltAmber)
    }
}

@Composable
private fun Visual04CoherenceDashboard(activeRun: PlaybookRunDoc?) {
    val ocs = LocalOcsColors.current
    val structural = activeRun?.structuralCoherence ?: 0.0
    val sonic = activeRun?.sonicCoherence ?: 0.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val diameter = size.height * 0.78f
        val leftTopLeft = Offset(size.width * 0.25f - diameter / 2f, size.height * 0.14f)
        val rightTopLeft = Offset(size.width * 0.75f - diameter / 2f, size.height * 0.14f)
        val arcSize = Size(diameter, diameter)

        // Structural Gauge (left)
        drawArc(ocs.hairline, 135f, 270f, false, leftTopLeft, arcSize, style = Stroke(18f, cap = StrokeCap.Round))
        drawArc(
            color = if (structural >= 0.85) ocs.structuralEmerald else ocs.kickCyan,
            startAngle = 135f,
            sweepAngle = (270f * structural.toFloat().coerceIn(0f, 1f)),
            useCenter = false,
            topLeft = leftTopLeft,
            size = arcSize,
            style = Stroke(18f, cap = StrokeCap.Round)
        )

        // Sonic Gauge (right — strictly separated per RULE-09)
        drawArc(ocs.hairline, 135f, 270f, false, rightTopLeft, arcSize, style = Stroke(18f, cap = StrokeCap.Round))
        drawArc(
            color = if (sonic >= 0.93) ocs.structuralEmerald else ocs.sonicViolet,
            startAngle = 135f,
            sweepAngle = (270f * sonic.toFloat().coerceIn(0f, 1f)),
            useCenter = false,
            topLeft = rightTopLeft,
            size = arcSize,
            style = Stroke(18f, cap = StrokeCap.Round)
        )
    }

    DualAxisCoherenceStrip(
        structuralValue = activeRun?.structuralCoherence,
        structuralStatus = activeRun?.structuralStatus ?: "pending_measurement",
        sonicValue = activeRun?.sonicCoherence,
        sonicStatus = activeRun?.sonicStatus ?: "open",
        invokedCount = activeRun?.invokedAtomicAgents?.size ?: 0,
        stepsDone = activeRun?.stepsDone ?: 0,
        driftCount = activeRun?.schemaDriftTypes?.size ?: 0
    )

    Spacer(Modifier.height(4.dp))
    Text(
        text = "Inherited v2.1 parent claims (0.97 / 0.91) are historical citations only and never re-asserted (RULE-04 / OD-11).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Visual05GroupPartition(activeRun: PlaybookRunDoc?) {
    val ocs = LocalOcsColors.current
    val invoked = activeRun?.invokedAtomicAgents?.toSet().orEmpty()
    val groups = listOf(
        CoreGroup.OrchestratorApex to ocs.structuralEmerald,
        CoreGroup.KickForge to ocs.kickCyan,
        CoreGroup.KickFlow to ocs.sonicViolet,
        CoreGroup.KickGuard to ocs.haltAmber
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { (group, accent) ->
            val members = ProtocolRegistry.agents.filter { it.group == group }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ocs.consoleSurface,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${group.label} (${members.size})", style = MaterialTheme.typography.titleSmall, color = accent)
                        Text(
                            text = "${members.count { it.name in invoked }}/${members.size} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(group.corePrimitives, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = members.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Text(
            text = "Authoritative Flow-Playbook v1.1 partition: 2 + 4 + 7 = 13 + Orchestrator Apex = 14 atomic agents.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Visual06EngineRegistry() {
    val ocs = LocalOcsColors.current
    val engines = listOf(
        Triple("⫻flow/meta-playbook", "Master 20-Step Framework (Default)", "6 Phases · 14 Atomic Agents · Structural floor 0.85") to ocs.kickCyan,
        Triple("⫻flow/molecule", "Compact Molecule Runtime (PTAS-06)", "Prepare → Combine → Validate → Retry → Finalize → Seal · Never a 2nd 20-step map") to ocs.structuralEmerald,
        Triple("⫻flow/qllickfizz_sonic", "Domain Engine (Music-Sonic / Lyria3)", "FORMAT-09 ⫻prompt/* · RULE-10 Consent Gate · Sonic OPEN until wet-lift (0.93)") to ocs.sonicViolet
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        engines.forEach { (info, accent) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ocs.consoleSurface,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(info.first, style = MaterialTheme.typography.labelLarge, color = accent)
                    Text(info.second, style = MaterialTheme.typography.titleSmall)
                    Text(info.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
