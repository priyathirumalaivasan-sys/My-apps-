package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.media.StudioSpeechAndAudioManager
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AccountAndAdminScreen
import com.example.ui.screens.AiToolsScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.StudioCreateScreen
import com.example.ui.theme.StudioAmbientBackdrop
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.AiToolsViewModel
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class StudioDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    STUDIO("studio", R.string.nav_studio, Icons.Rounded.MovieCreation, Icons.Outlined.MovieCreation),
    PROJECTS("projects", R.string.nav_projects, Icons.Rounded.Folder, Icons.Outlined.Folder),
    GALLERY("gallery", R.string.nav_gallery, Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary),
    AI_TOOLS("ai_tools", R.string.nav_assist, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    ACCOUNT("account", R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainStudioShell(currentUserId = user.uid)
        }
    }
}

@Composable
fun MainStudioShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val studioViewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = StudioViewModel.provideFactory(currentUserId)
    )
    val aiToolsViewModel: AiToolsViewModel = viewModel(key = "ai_$currentUserId")
    val audioManager = remember(context) { StudioSpeechAndAudioManager(context) }

    DisposableEffect(audioManager) {
        onDispose { audioManager.release() }
    }

    val banner by studioViewModel.actionBanner.collectAsState()
    LaunchedEffect(banner) {
        val msg = banner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            studioViewModel.clearBanner()
        }
    }

    var currentDestination by rememberSaveable { mutableStateOf(StudioDestination.STUDIO) }

    BackHandler(enabled = currentDestination != StudioDestination.STUDIO) {
        currentDestination = StudioDestination.STUDIO
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        StudioAmbientBackdrop {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = StudioSurface.copy(alpha = 0.96f),
                            tonalElevation = 8.dp
                        ) {
                            StudioDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = StudioYellow,
                                        selectedTextColor = StudioYellow,
                                        indicatorColor = StudioPurple.copy(alpha = 0.28f),
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = StudioSurface.copy(alpha = 0.96f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            StudioDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                val label = stringResource(dest.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = StudioYellow,
                                        selectedTextColor = StudioYellow,
                                        indicatorColor = StudioPurple.copy(alpha = 0.28f),
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Crossfade(targetState = currentDestination, label = "studio_tab") { dest ->
                            when (dest) {
                                StudioDestination.STUDIO -> StudioCreateScreen(
                                    studioViewModel = studioViewModel,
                                    aiToolsViewModel = aiToolsViewModel,
                                    audioManager = audioManager,
                                    onNavigateToSettings = { currentDestination = StudioDestination.ACCOUNT },
                                    onNavigateToGallery = { currentDestination = StudioDestination.GALLERY }
                                )
                                StudioDestination.PROJECTS -> ProjectsScreen(
                                    studioViewModel = studioViewModel,
                                    aiToolsViewModel = aiToolsViewModel,
                                    onOpenInStudio = { currentDestination = StudioDestination.STUDIO }
                                )
                                StudioDestination.GALLERY -> GalleryScreen(
                                    studioViewModel = studioViewModel,
                                    audioManager = audioManager,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                                StudioDestination.AI_TOOLS -> AiToolsScreen(
                                    studioViewModel = studioViewModel,
                                    aiToolsViewModel = aiToolsViewModel,
                                    audioManager = audioManager,
                                    onSendToStudio = { currentDestination = StudioDestination.STUDIO },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                                StudioDestination.ACCOUNT -> AccountAndAdminScreen(
                                    studioViewModel = studioViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
