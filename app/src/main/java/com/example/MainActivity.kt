package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.EnginesScreen
import com.example.ui.screens.GovernScreen
import com.example.ui.screens.VisualsScreen
import com.example.ui.theme.LocalOcsColors
import com.example.ui.theme.OcsRuntimeTheme
import com.example.ui.viewmodel.OcsRuntimeViewModel
import com.example.ui.viewmodel.StreamState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent BEFORE Compose UI, ViewModels, or Firebase ops.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            OcsRuntimeTheme {
                OcsAppRoot()
            }
        }
    }
}

private data class NavDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val navDestinations = listOf(
    NavDestination("Cockpit", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_cockpit"),
    NavDestination("Engines", Icons.Filled.Hub, Icons.Outlined.Hub, "nav_engines"),
    NavDestination("Visuals", Icons.Filled.AccountTree, Icons.Outlined.AccountTree, "nav_visuals"),
    NavDestination("Govern", Icons.Filled.Gavel, Icons.Outlined.Gavel, "nav_govern")
)

@Composable
private fun OcsAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedRuntimeShell(
                userId = uid,
                userEmail = currentUser?.email ?: "Operator",
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedRuntimeShell(
    userId: String,
    userEmail: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val viewModel: OcsRuntimeViewModel = viewModel(
        key = userId,
        factory = OcsRuntimeViewModel.provideFactory(context)
    )

    val runsState by viewModel.runsState.collectAsStateWithLifecycle()
    val stepsState by viewModel.stepsState.collectAsStateWithLifecycle()
    val selectedRunId by viewModel.selectedRunId.collectAsStateWithLifecycle()
    val moleculesState by viewModel.moleculesState.collectAsStateWithLifecycle()
    val sonicRunsState by viewModel.sonicRunsState.collectAsStateWithLifecycle()
    val decisionsState by viewModel.decisionsState.collectAsStateWithLifecycle()
    val sealedSiblingsState by viewModel.sealedSiblingsState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val ocs = LocalOcsColors.current

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(actionState.bannerMessage) {
        val msg = actionState.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val runs = (runsState as? StreamState.Success)?.data.orEmpty()
    val steps = (stepsState as? StreamState.Success)?.data.orEmpty()
    val activeRun = runs.firstOrNull { it.id == selectedRunId } ?: runs.firstOrNull()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "⫻ OCS RUNTIME · v2.2",
                            style = MaterialTheme.typography.titleMedium,
                            color = ocs.kickCyan
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                signOutUser(credentialManager, scope, onSignOutComplete = onSignedOut)
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign out $userEmail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navDestinations.forEachIndexed { idx, dest ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == idx) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        navDestinations.forEachIndexed { idx, dest ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == idx) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> CockpitScreen(
                            runsState = runsState,
                            stepsState = stepsState,
                            selectedRunId = selectedRunId,
                            actionState = actionState,
                            viewModel = viewModel,
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                        1 -> EnginesScreen(
                            moleculesState = moleculesState,
                            sonicRunsState = sonicRunsState,
                            actionState = actionState,
                            viewModel = viewModel,
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                        2 -> VisualsScreen(
                            activeRun = activeRun,
                            steps = steps
                        )
                        else -> GovernScreen(
                            decisionsState = decisionsState,
                            sealedSiblingsState = sealedSiblingsState,
                            viewModel = viewModel,
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }
            }
        }
    }
}
