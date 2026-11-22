package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalOcsColors
import java.util.Locale

@Composable
fun DisplayHeaderCard(
    executable: String,
    input: String,
    expectedOutput: String,
    modifier: Modifier = Modifier
) {
    val ocs = LocalOcsColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ocs.kickCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ocs.kickCyan)
                )
                Text(
                    text = "⫻display/header · FORMAT-08",
                    style = MaterialTheme.typography.labelMedium,
                    color = ocs.kickCyan
                )
            }
            Text(
                text = "Executable: $executable",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Input: $input",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Expected output: $expectedOutput",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DisplayFooterCard(
    contextNote: String,
    summary: String,
    nextAction: String,
    modifier: Modifier = Modifier
) {
    val ocs = LocalOcsColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ocs.hairline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⫻display/footer",
                style = MaterialTheme.typography.labelMedium,
                color = ocs.haltAmber
            )
            Text("Context: $contextNote", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Summary: $summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Next action: $nextAction", style = MaterialTheme.typography.labelMedium, color = ocs.kickCyan)
        }
    }
}

@Composable
fun DualAxisCoherenceStrip(
    structuralValue: Double?,
    structuralStatus: String,
    sonicValue: Double?,
    sonicStatus: String,
    invokedCount: Int,
    stepsDone: Int,
    driftCount: Int,
    modifier: Modifier = Modifier
) {
    val ocs = LocalOcsColors.current
    val structuralPassing = (structuralValue ?: 0.0) >= 0.85
    val structuralColor = when {
        structuralValue == null -> MaterialTheme.colorScheme.onSurfaceVariant
        structuralPassing -> ocs.structuralEmerald
        else -> ocs.haltAmber
    }
    val sonicColor = when {
        sonicValue == null -> ocs.sonicViolet
        sonicValue >= 0.93 -> ocs.structuralEmerald
        else -> ocs.haltAmber
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Structural Axis Card (Never collapsed with sonic per RULE-09)
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, structuralColor.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("STRUCTURAL AXIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = structuralValue?.let { String.format(Locale.US, "%.3f", it) } ?: "PENDING",
                    style = MaterialTheme.typography.titleLarge,
                    color = structuralColor
                )
                LinearProgressIndicator(
                    progress = { (structuralValue ?: 0.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = structuralColor,
                    trackColor = ocs.hairline
                )
                Text(
                    text = "Floor 0.85 · $structuralStatus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sonic Axis Card (Stays OPEN until explicit wet-lift per RULE-09)
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, sonicColor.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SONIC AXIS (RULE-09)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = sonicValue?.let { String.format(Locale.US, "%.3f", it) } ?: "OPEN",
                    style = MaterialTheme.typography.titleLarge,
                    color = sonicColor
                )
                LinearProgressIndicator(
                    progress = { (sonicValue ?: 0.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = sonicColor,
                    trackColor = ocs.hairline
                )
                Text(
                    text = "Target 0.93 · $sonicStatus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TelemetryPill("Steps $stepsDone/20", ocs.kickCyan)
        TelemetryPill("Agents $invokedCount/14", if (invokedCount >= 12) ocs.structuralEmerald else ocs.kickCyan)
        TelemetryPill(
            text = if (driftCount == 0) "Drift: CLEAN" else "Drift: $driftCount",
            accent = if (driftCount == 0) ocs.structuralEmerald else ocs.driftCrimson
        )
        TelemetryPill("Proto v2.2", ocs.haltAmber)
    }
}

@Composable
fun TelemetryPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun KickLangCodeViewer(
    code: String,
    title: String = "KickLang ⫻ Output",
    onCopied: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ocs = LocalOcsColors.current
    val clipboard = LocalClipboardManager.current
    val cyan = ocs.kickCyan
    val amber = ocs.haltAmber
    val violet = ocs.sonicViolet
    val emerald = ocs.structuralEmerald
    val ink = MaterialTheme.colorScheme.onSurface

    val highlighted = remember(code, cyan, amber, violet, emerald, ink) {
        buildAnnotatedString {
            code.lines().forEachIndexed { idx, line ->
                val trimmed = line.trimStart()
                when {
                    trimmed.startsWith("⫻cmd/halt") || trimmed.startsWith("⫻logic/error") -> {
                        withStyle(SpanStyle(color = amber, fontWeight = FontWeight.Bold)) { append(line) }
                    }
                    trimmed.startsWith("⫻prompt/") || trimmed.startsWith("⫻role/") || trimmed.startsWith("⫻flow/qllickfizz") -> {
                        withStyle(SpanStyle(color = violet, fontWeight = FontWeight.SemiBold)) { append(line) }
                    }
                    trimmed.startsWith("⫻data/var") || trimmed.startsWith("⫻end/") -> {
                        withStyle(SpanStyle(color = emerald, fontWeight = FontWeight.Medium)) { append(line) }
                    }
                    trimmed.startsWith("⫻") -> {
                        withStyle(SpanStyle(color = cyan, fontWeight = FontWeight.SemiBold)) { append(line) }
                    }
                    else -> {
                        withStyle(SpanStyle(color = ink)) { append(line) }
                    }
                }
                if (idx < code.lines().lastIndex) append("\n")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ocs.consoleSurface)
            .border(1.dp, ocs.hairline, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = ocs.kickCyan, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    onCopied()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy KickLang transcript",
                    tint = ocs.kickCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = highlighted,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.5.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

@Composable
fun ErrorTelemetryBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ocs = LocalOcsColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ocs.driftCrimsonDim,
        border = BorderStroke(1.dp, ocs.driftCrimson)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = ocs.driftCrimson, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss error", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
