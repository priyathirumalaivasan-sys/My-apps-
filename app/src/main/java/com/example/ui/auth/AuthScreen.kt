package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GradientStudioButton
import com.example.ui.theme.StudioAmbientBackdrop
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioYellow

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    StudioAmbientBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, StudioBlack.copy(alpha = 0.9f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "VASAN AI STUDIO",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioTextPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioYellow
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentHighlight = true
                ) {
                    StudioStatusBadge(
                        text = "MULTI-MODEL VIDEO SUITE",
                        color = StudioPurpleGlow
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Director-Grade AI Video Creation",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StudioTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Orchestrate Kling AI, MiniMax Hailuo, Google Veo, and Runway AI with Gemini prompt engineering, Tamil & English scripting, AI thumbnails, and cloud project sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary
                    )

                    Spacer(Modifier.height(16.dp))
                    FeatureHighlightRow(
                        icon = Icons.Rounded.MovieCreation,
                        text = "Text, Image & Video-to-Video + Cartoon & Character Animation"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Rounded.Translate,
                        text = "Bilingual Tamil & English AI Prompt & Script Assistant"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Rounded.AutoAwesome,
                        text = "AI Image & Thumbnail Generator + Voiceover TTS"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Rounded.Shield,
                        text = "Zero-exposure backend proxy — API keys stay off your device"
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioError
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    GradientStudioButton(
                        text = stringResource(R.string.sign_in_with_google),
                        icon = Icons.Rounded.AutoAwesome,
                        isLoading = isLoading,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_sign_in_button")
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Secured via Google Sign-In & Firebase Auth. New accounts start with 100 studio credits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StudioYellow,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextPrimary
        )
    }
}
