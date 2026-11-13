package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.model.AiVideoModel
import com.example.data.model.CreditTransaction
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.auth.signOut
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GradientStudioButton
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioOptionChip
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun AccountAndAdminScreen(
    studioViewModel: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileState by studioViewModel.userProfile.collectAsState()
    val settings by studioViewModel.settings.collectAsState()
    val backendHealth by studioViewModel.backendHealth.collectAsState()
    val txState by studioViewModel.creditTransactions.collectAsState()
    val adminUsersState by studioViewModel.adminUsers.collectAsState()

    var backendUrlInput by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }

    val profile = (profileState as? UiState.Success)?.data
    val isAdmin = profile?.role == "admin" ||
        profile?.email.equals("priyathirumalaivasan@gmail.com", ignoreCase = true)

    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            studioViewModel.loadAdminUsers()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(
                title = "Account, Models & Settings",
                subtitle = "Profile, credits ledger, Express backend config & admin tools"
            )

            // User Profile & Credits Card
            GlassCard(accentHighlight = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = StudioSurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = StudioYellow)
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "Studio Creator",
                            style = MaterialTheme.typography.titleLarge,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = profile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioStatusBadge(
                                text = "${profile?.credits ?: 0} Credits",
                                color = StudioYellow
                            )
                            StudioStatusBadge(
                                text = (profile?.plan ?: "starter").uppercase(),
                                color = StudioPurpleGlow
                            )
                            if (isAdmin) {
                                StudioStatusBadge(text = "ADMIN", color = StudioSuccess)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { signOut(credentialManager, scope) },
                    border = BorderStroke(1.dp, StudioError.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioError),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }

            // Node.js Express Backend Configuration
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Node.js Express Backend",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary
                    )
                    StudioStatusBadge(
                        text = if (backendHealth.isReachable) "ONLINE" else "OFFLINE",
                        color = if (backendHealth.isReachable) StudioSuccess else StudioWarning
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "API keys for Kling, MiniMax, Google Veo, and Runway live strictly inside /backend/.env on your Express server — never in the Android APK.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = { backendUrlInput = it },
                    label = { Text("Backend Base URL") },
                    placeholder = { Text("https://your-service.run.app or http://10.0.2.2:3000") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backend_url_input"),
                    colors = studioTextFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { backendUrlInput = "http://10.0.2.2:3000" },
                        border = BorderStroke(1.dp, StudioPurpleGlow.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emulator Local", color = StudioPurpleGlow)
                    }
                    GradientStudioButton(
                        text = "Save & Test",
                        icon = Icons.Rounded.Save,
                        isLoading = backendHealth.isChecking,
                        onClick = { studioViewModel.saveBackendUrl(backendUrlInput) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_backend_url_button")
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (backendHealth.isReachable) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (backendHealth.isReachable) StudioSuccess else StudioTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = backendHealth.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary
                    )
                }
            }

            // AI Models Matrix
            GlassCard {
                Text(
                    text = "AI Video Models Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                Spacer(Modifier.height(10.dp))
                AiVideoModel.entries.forEach { model ->
                    val configured = backendHealth.health?.providers?.get(model.displayName) == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.displayName, style = MaterialTheme.typography.labelLarge, color = StudioTextPrimary)
                            Text(
                                text = "Backend keys: ${model.envKeys.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextMuted
                            )
                        }
                        StudioStatusBadge(
                            text = if (configured) "Configured" else "Missing Keys",
                            color = if (configured) StudioSuccess else StudioWarning
                        )
                    }
                }
            }

            // Admin Dashboard (for verified admin priyathirumalaivasan@gmail.com or /admins/{uid})
            if (isAdmin) {
                GlassCard(accentHighlight = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = StudioYellow)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioYellow
                            )
                        }
                        OutlinedButton(
                            onClick = { studioViewModel.loadAdminUsers() },
                            border = BorderStroke(1.dp, StudioYellow.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync", color = StudioYellow)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (val aState = adminUsersState) {
                        is UiState.Loading -> CircularProgressIndicator(color = StudioYellow, modifier = Modifier.size(24.dp))
                        is UiState.Error -> Text(aState.message, color = StudioError)
                        is UiState.Success -> {
                            aState.data.forEach { user ->
                                AdminUserRow(
                                    user = user,
                                    onGrant100 = {
                                        studioViewModel.adminGrantCredits(
                                            targetUserId = user.userId,
                                            newCredits = user.credits + 100L,
                                            plan = "pro",
                                            reason = "Admin credit grant (+100)"
                                        )
                                    }
                                )
                            }
                        }
                        UiState.Idle -> Unit
                    }
                }
            }

            // Credit Ledger
            GlassCard {
                Text(
                    text = "Recent Credit Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                when (val tState = txState) {
                    is UiState.Success -> {
                        if (tState.data.isEmpty()) {
                            Text("No transactions recorded yet.", style = MaterialTheme.typography.bodyMedium, color = StudioTextMuted)
                        } else {
                            tState.data.take(10).forEach { tx: CreditTransaction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tx.reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StudioTextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (tx.amount >= 0) "+${tx.amount}" else "${tx.amount}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (tx.amount >= 0) StudioSuccess else StudioYellow
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> Text(tState.message, color = StudioError)
                    else -> Text("Loading ledger...", style = MaterialTheme.typography.bodyMedium, color = StudioTextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminUserRow(
    user: UserProfile,
    onGrant100: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.labelLarge, color = StudioTextPrimary)
            Text("${user.email} • ${user.credits} cr (${user.plan})", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        }
        OutlinedButton(
            onClick = onGrant100,
            border = BorderStroke(1.dp, StudioYellow)
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("+100 Pro", color = StudioYellow)
        }
    }
}
