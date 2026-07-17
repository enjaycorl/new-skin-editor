package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Developer ─────────────────────────────────────────────────────
            SettingsSection(title = "Developer") {
                DeveloperCard()
            }

            // ── App Info ──────────────────────────────────────────────────────
            SettingsSection(title = "App Info") {
                SettingsInfoRow(
                    icon = Icons.Default.Brush,
                    label = "Application",
                    value = "SkinCraft Studio"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsInfoRow(
                    icon = Icons.Default.Tag,
                    label = "Version",
                    value = "1.0.0"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsInfoRow(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Platform",
                    value = "Android (Jetpack Compose)"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsInfoRow(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Engine",
                    value = "Gemini (Firebase AI)"
                )
            }

            // ── Features ──────────────────────────────────────────────────────
            SettingsSection(title = "Features") {
                SettingsFeatureRow(
                    icon = Icons.Default.GridOn,
                    title = "2D Skin Painter",
                    description = "Pixel-by-pixel editor with brush, eraser, fill & eyedropper"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsFeatureRow(
                    icon = Icons.Default.Layers,
                    title = "3D Preview Studio",
                    description = "Directional-lit voxel renderer with poses, animations & environments"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Skin Generator",
                    description = "Generate Minecraft skins from text prompts via Gemini"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SettingsFeatureRow(
                    icon = Icons.Default.SaveAlt,
                    title = "Export & Share",
                    description = "Save to gallery, share textures or export animated GIFs"
                )
            }

            // ── Skin Formats ──────────────────────────────────────────────────
            SettingsSection(title = "Supported Skin Formats") {
                listOf(
                    Triple("64 × 32", "Legacy classic format", Icons.Default.CropSquare),
                    Triple("64 × 64", "Modern standard format", Icons.Default.CropSquare),
                    Triple("128 × 128", "HD high-resolution format", Icons.Default.CropSquare)
                ).forEachIndexed { i, (fmt, desc, icon) ->
                    if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    SettingsInfoRow(icon = icon, label = fmt, value = desc)
                }
            }

            // ── Credits ───────────────────────────────────────────────────────
            SettingsSection(title = "Credits & Technologies") {
                listOf(
                    "Jetpack Compose — UI framework",
                    "Material Design 3 — Design system",
                    "Firebase AI (Gemini) — AI generation",
                    "Room Database — Local persistence",
                    "Coil — Image loading",
                    "OkHttp / Retrofit — Networking"
                ).forEachIndexed { i, credit ->
                    if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(credit, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Bottom spacer
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Developer spotlight card ───────────────────────────────────────────────────

@Composable
private fun DeveloperCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(3.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Op",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "Opiar",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Lead Developer",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Role badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeveloperBadge(icon = Icons.Default.Code, label = "Kotlin")
                DeveloperBadge(icon = Icons.Default.Palette, label = "UI / UX")
                DeveloperBadge(icon = Icons.Default.AutoAwesome, label = "AI")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "Designed and built SkinCraft Studio — a full-featured Minecraft skin editor with AI generation, real-time 3D preview, and seamless export.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RowScope.DeveloperBadge(icon: ImageVector, label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── Reusable section containers & rows ────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsFeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
