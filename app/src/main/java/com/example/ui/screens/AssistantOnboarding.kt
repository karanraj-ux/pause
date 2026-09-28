package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.viewmodels.SettingsViewModel
import com.example.Screen

@Composable
fun AssistantOnboardingScreen(onComplete: (String?) -> Unit) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val uiState by settingsViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Shield Header (Premium)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(24.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uiState.assistantAvatar,
                    contentDescription = "Assistant Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Hi, I'm ${uiState.assistantName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "${uiState.assistantName} isn't an unpredictable AI. It is a powerful automation engine built on your exact rules. Flawlessly executed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Capabilities (Grid-like layout using Rows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TabCard(
                        title = "Focus Shield",
                        description = "Reject unknown WhatsApp & phone calls automatically.",
                        icon = Icons.Default.Security,
                        iconColor = MaterialTheme.colorScheme.error,
                        isCompact = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabCard(
                        title = "Starred Bypass",
                        description = "Bypass DND & silent mode for chosen starred contacts.",
                        icon = Icons.Default.Star,
                        iconColor = MaterialTheme.colorScheme.primary,
                        isCompact = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TabCard(
                        title = "Auto-Reply",
                        description = "Instant reply over internet for WhatsApp & SMS.",
                        icon = Icons.Default.ChatBubble,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        isCompact = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabCard(
                        title = "Schedule SMS",
                        description = "Local on-device scheduled SMS without cloud servers.",
                        icon = Icons.Default.Schedule,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        isCompact = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            FilledTonalButton(
                onClick = { onComplete(Screen.Dashboard.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start Customizing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TabCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isCompact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (isCompact) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
