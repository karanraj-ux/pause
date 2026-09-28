package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeOnboardingScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Premium Hero Section with gradients and overlapping elements
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background decorative circles
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = (-40).dp)
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .offset(x = 100.dp, y = 60.dp)
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f), CircleShape)
            )
            
            // Central Icon Composition
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Pause",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your sovereign call gatekeeper, DND VIP bypass, and automated focus responder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WelcomeFeatureItem(
                    icon = Icons.Default.Security,
                    title = "Focus Shield & Call Gatekeeper",
                    description = "Reject unknown WhatsApp & phone calls automatically when focus mode is on."
                )
                WelcomeFeatureItem(
                    icon = Icons.Rounded.VpnKey,
                    title = "Starred Contact DND Bypass",
                    description = "Allow Starred family & VIP contacts to break through silent mode in emergencies."
                )
                WelcomeFeatureItem(
                    icon = Icons.Default.CloudOff,
                    title = "Zero Cloud & 100% Offline Core",
                    description = "Hardware-encrypted local database. No cloud tracking or telemetry."
                )
            }
        }
        
        // Fixed Bottom Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "By continuing, you agree that your data remains purely local.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            FilledTonalButton(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun WelcomeFeatureItem(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
