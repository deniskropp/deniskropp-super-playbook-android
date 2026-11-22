package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.domain.ProtocolRegistry
import com.example.ui.theme.LocalOcsColors

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val ocsColors = LocalOcsColors.current
    val colorScheme = MaterialTheme.colorScheme

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAuto by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ocsColors.kickCyan.copy(alpha = 0.14f),
                            colorScheme.background
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.maxDimension * 0.7f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ocsColors.sonicViolet.copy(alpha = 0.10f),
                            colorScheme.background.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.1f, size.height * 0.9f),
                        radius = size.maxDimension * 0.55f
                    )
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // FORMAT-08 Session Bootstrap Header
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, ocsColors.kickCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⫻display/header · SESSION BOOTSTRAP",
                        style = MaterialTheme.typography.labelMedium,
                        color = ocsColors.kickCyan
                    )
                    Text(
                        text = "Executable: Authenticate OCS/Display Operator",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Input: Google Identity Credential (Credential Manager)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Expected output: Scoped Cloud Runtime (⫻protocol/ocs v${ProtocolRegistry.PROTOCOL_VERSION})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "⫻ OCS RUNTIME",
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.onBackground
                )
                Text(
                    text = "${ProtocolRegistry.DOCUMENT_VERSION} · ${ProtocolRegistry.LANGUAGE_MODEL}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ocsColors.haltAmber
                )
                Text(
                    text = "Full 14-agent Gemini swarm runtime with deterministic RULE-01..11 validation, dual-axis coherence telemetry, AR-00L visualizers, and immutable RULE-11 sibling seals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, ocsColors.hairline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CapabilityRow(Icons.Default.Hub, "3 Flow Engines", "20-Step Meta-Playbook, Compact Molecule, Sonique Lyria3")
                    CapabilityRow(Icons.Default.Security, "Hardened Governance", "RULE-10 consent gates, Open Decisions 1–12 & SP-A..SP-H")
                    CapabilityRow(Icons.Default.Tune, "Dual-Axis Telemetry", "Structural >= 0.85 floor; Sonic OPEN until wet-lift (0.93)")
                    CapabilityRow(Icons.Default.CheckCircle, "Cloud-Synced Seals", "Per-user Firestore isolation & immutable RULE-11 siblings")
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ocsColors.driftCrimsonDim,
                    border = BorderStroke(1.dp, ocsColors.driftCrimson)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading && !isCheckingAuto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ocsColors.kickCyan,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                if (isLoading || isCheckingAuto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isCheckingAuto) "Checking Operator Session..." else "Authenticating...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Sign-In icon",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "⫻display/footer · Protocol stays ⫻protocol/ocs v2.2 · OPT-D partial · v3.0 reserved",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapabilityRow(icon: ImageVector, title: String, subtitle: String) {
    val ocsColors = LocalOcsColors.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ocsColors.kickCyan,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
