package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SkinProject
import com.example.ui.SkinCraftViewModel
import com.example.utils.SkinTextureMapper
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: SkinCraftViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLookupDialog by remember { mutableStateOf(false) }

    // Display any status toast messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Media file picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importSkinFromGallery(context, it, "Imported Skin")
            onNavigateToEditor()
        }
    }

    // Filter projects based on query and folders
    val filteredProjects = remember(projects, searchQuery, selectedFolder) {
        projects.filter { proj ->
            val matchQuery = proj.name.contains(searchQuery, ignoreCase = true)
            val matchFolder = when (selectedFolder) {
                "All" -> true
                "Favorites" -> proj.isFavorite
                else -> proj.folder.equals(selectedFolder, ignoreCase = true)
            }
            matchQuery && matchFolder
        }
    }

    val folders = remember(projects) {
        listOf("All", "Favorites") + projects.map { it.folder }.distinct().filter { it != "My Skins" && it != "Starter Templates" && it != "Imported" }.sorted() + listOf("My Skins", "Starter Templates", "Imported")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SkinCraft Studio",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_nav_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_skin_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Skin")
            }
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
        ) {
            // Search and quick action header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search skins & presets...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_skins_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Import Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showLookupDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_lookup_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Player Lookup", fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/png") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_import_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import PNG", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            // Folders Horizontal list
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    val isSelected = selectedFolder == folder
                    InputChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loading / Empty States / Grid list
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Skins Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the '+' button below to start a blank skin or import one from Mojang player galleries.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredProjects) { project ->
                        SkinProjectCard(
                            project = project,
                            onClick = {
                                viewModel.selectProject(project)
                                onNavigateToEditor()
                            },
                            onDelete = {
                                viewModel.deleteProject(project)
                            },
                            onFavorite = {
                                viewModel.toggleFavorite(project)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create New Skin
    if (showCreateDialog) {
        var nameInput by remember { mutableStateOf("") }
        var selectedFormat by remember { mutableStateOf("64x64") }
        var selectedModel by remember { mutableStateOf("STEVE") }
        var selectedBase by remember { mutableStateOf("Steve") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Craft New Skin", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Skin Name") },
                        placeholder = { Text("E.g., Ender Knight") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_skin_name_input")
                    )

                    Text("Model Layout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            onClick = { selectedModel = "STEVE" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedModel == "STEVE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Steve Model", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("4px Standard Arm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            onClick = { selectedModel = "ALEX" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedModel == "ALEX") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Alex Model", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("3px Slim Arm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text("Skin Format", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("64x32", "64x64", "128x128").forEach { format ->
                            FilterChip(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format },
                                label = { Text(format) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Base Template", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Steve", "Alex", "Creeper", "Zombie", "Enderman", "Knight").forEach { base ->
                            FilterChip(
                                selected = selectedBase == base,
                                onClick = { selectedBase = base },
                                label = { Text(base, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(nameInput, selectedFormat, selectedModel, selectedBase)
                        showCreateDialog = false
                        onNavigateToEditor()
                    },
                    modifier = Modifier.testTag("dialog_create_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Player Username Lookup
    if (showLookupDialog) {
        var usernameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLookupDialog = false },
            title = { Text("Player Skin Search", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter any official Minecraft Java Edition player username to import their current skin texture directly.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Minecraft Username") },
                        placeholder = { Text("E.g., Notch") },
                        modifier = Modifier.fillMaxWidth().testTag("lookup_username_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (usernameInput.trim().isNotEmpty()) {
                            viewModel.importPlayerSkin(usernameInput.trim())
                            showLookupDialog = false
                            onNavigateToEditor()
                        }
                    },
                    modifier = Modifier.testTag("lookup_confirm_button")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLookupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SkinProjectCard(
    project: SkinProject,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .testTag("skin_project_card_${project.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2D Skin Head Preview Canvas
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SkinHeadPreview(
                        textureBytes = project.textureData,
                        format = project.format,
                        modelType = project.modelType,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = project.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.format,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = project.modelType,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (project.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (project.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_skin_button_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkinHeadPreview(
    textureBytes: ByteArray,
    format: String,
    modelType: String,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(textureBytes) {
        try {
            BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    if (decodedBitmap == null) {
        Icon(Icons.Default.Face, contentDescription = null, modifier = modifier, tint = MaterialTheme.colorScheme.primary)
        return
    }

    Canvas(modifier = modifier) {
        val w = decodedBitmap.width
        val h = decodedBitmap.height

        // Extract Head Front Face Box from the Bitmap
        // 64x64/64x32 mapping for head front is at x=8, y=8, size=8x8 pixels.
        // For 128x128 HD, it is x=16, y=16, size=16x16.
        val scalar = if (format == "128x128") 2 else 1
        val headX = 8 * scalar
        val headY = 8 * scalar
        val headSize = 8 * scalar

        val cellW = size.width / headSize
        val cellH = size.height / headSize

        // Double check bounds
        if (headX + headSize <= w && headY + headSize <= h) {
            val pixels = IntArray(headSize * headSize)
            decodedBitmap.getPixels(pixels, 0, headSize, headX, headY, headSize, headSize)

            for (dy in 0 until headSize) {
                for (dx in 0 until headSize) {
                    val color = pixels[dy * headSize + dx]
                    val alpha = (color ushr 24) and 0xFF
                    if (alpha > 0) {
                        drawRect(
                            color = Color(color),
                            topLeft = Offset(dx * cellW, dy * cellH),
                            size = Size(cellW + 0.5f, cellH + 0.5f) // overlapping draws to avoid lines
                        )
                    }
                }
            }

            // Draw Outer overlay layer (accessory Hat) if present and has colored pixels
            // Hat Front mapping: x=40, y=8, size=8x8.
            val hatX = 40 * scalar
            val hatY = 8 * scalar
            if (hatX + headSize <= w && hatY + headSize <= h) {
                val hatPixels = IntArray(headSize * headSize)
                decodedBitmap.getPixels(hatPixels, 0, headSize, hatX, hatY, headSize, headSize)

                for (dy in 0 until headSize) {
                    for (dx in 0 until headSize) {
                        val color = hatPixels[dy * headSize + dx]
                        val alpha = (color ushr 24) and 0xFF
                        if (alpha > 5) { // Ensure visible
                            drawRect(
                                color = Color(color),
                                topLeft = Offset(dx * cellW, dy * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f)
                            )
                        }
                    }
                }
            }
        } else {
            // Fallback
            drawRect(Color.Gray)
        }
    }
}
