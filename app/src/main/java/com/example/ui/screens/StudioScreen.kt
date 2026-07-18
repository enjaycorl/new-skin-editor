package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SkinProject
import com.example.ui.SkinCraftViewModel
import com.example.utils.BodyPart
import com.example.utils.EnvironmentType
import com.example.utils.FaceType
import com.example.utils.LayerType
import com.example.utils.ModelPose
import com.example.utils.Skin3DRenderer
import com.example.utils.SkinExportUtils
import com.example.utils.SkinTextureMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: SkinCraftViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val project by viewModel.currentProject.collectAsState()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No project selected", color = Color.White)
        }
        return
    }

    val activeProject = project!!
    var selectedTab by remember { mutableStateOf(1) } // 0 = 2D Paint, 1 = 3D Studio (default to 3D for scenic view)

    // 3D camera variables
    var pitch by remember { mutableStateOf(-0.2f) }
    var yaw by remember { mutableStateOf(0.5f) }
    var zoom3D by remember { mutableStateOf(1.0f) }
    var autoRotate by remember { mutableStateOf(false) }

    // Grid overlays
    var show2DGrid by remember { mutableStateOf(true) }
    var show3DGrid by remember { mutableStateOf(true) }

    // Outer/Inner body layers visibility in 3D
    var showOuterLayerOnly by remember { mutableStateOf(true) }

    // Floating panels toggle
    var showBodyPanel by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Dialog flags
    var showExportSuccess by remember { mutableStateOf(false) }
    var exportedUriString by remember { mutableStateOf("") }
    var showFormatDialog by remember { mutableStateOf(false) }

    val activeTool by viewModel.selectedTool.collectAsState()
    val activeColor by viewModel.activeColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()

    val activePart by viewModel.activeBodyPart.collectAsState()
    val activeFace by viewModel.activeFace.collectAsState()
    val activeLayer by viewModel.activeLayer.collectAsState()
    val pixels by viewModel.editorPixels.collectAsState()

    val isAnimating by viewModel.is3DAnimating.collectAsState()
    val mirrorMode by viewModel.mirrorMode.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    var activeTime by remember { mutableStateOf(0f) }

    // Gallery / file import launcher
    var showImportOptions by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importSkinFromGallery(context, it, "Imported Skin") }
    }

    val mapping = remember(activePart, activeFace, activeLayer, activeProject) {
        SkinTextureMapper.getScaledFaceMapping(
            activePart,
            activeLayer,
            activeFace,
            activeProject.modelType,
            activeProject.format
        )
    }

    // Auto-rotate logic
    LaunchedEffect(autoRotate) {
        while (autoRotate) {
            yaw += 0.018f
            delay(16)
        }
    }

    // Walking animation logic
    if (isAnimating) {
        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (true) {
                activeTime = ((System.currentTimeMillis() - start) / 1000f) * 4f
                delay(16)
            }
        }
    }

    val outerToggles = remember(showOuterLayerOnly) {
        BodyPart.values().associateWith { showOuterLayerOnly }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Beautiful Background Image (Minecraft Landscape)
        Image(
            painter = painterResource(id = R.drawable.img_minecraft_background_1784324712218),
            contentDescription = "Minecraft World Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 2. Custom Top Header Bar from image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Download + Import (stacked)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Download row
                        Row(
                            modifier = Modifier
                                .clickable {
                                    val uri = SkinExportUtils.saveSkinToGallery(
                                        context,
                                        viewModel.editorPixels.value,
                                        activeProject.format,
                                        activeProject.name
                                    )
                                    if (uri != null) {
                                        exportedUriString = uri.toString()
                                        showExportSuccess = true
                                    } else {
                                        Toast.makeText(context, "Export failed. Check permissions.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("download_top_bar_button")
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("EXPORT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        // Import row
                        Row(
                            modifier = Modifier
                                .clickable { showImportOptions = true }
                                .testTag("import_top_bar_button")
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import", tint = Color(0xFF80CBC4), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("IMPORT", color = Color(0xFF80CBC4), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Center Column: Title & Username
                    Column(
                        modifier = Modifier.weight(1.8f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.saveActiveSkin()
                                        onNavigateBack()
                                    }
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("back_to_library_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Skin Editor",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeProject.name,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Right Column: Save
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                coroutineScope.launch {
                                    viewModel.saveActiveSkin()
                                    Toast.makeText(context, "Skin Saved!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("save_top_bar_button"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Skin",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "SAVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                // Thin bottom divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }

            // 3. Central Editing Viewport (holds 3D Render or 2D Painter Grid)
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 1) {
                    // 3D Preview Canvas full screen in background of viewport
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoomFactor, _ ->
                                    yaw -= pan.x * 0.012f
                                    pitch = (pitch + pan.y * 0.012f).coerceIn(-1.5f, 1.5f)
                                    zoom3D = (zoom3D * zoomFactor).coerceIn(0.5f, 2.0f)
                                }
                            }
                            .testTag("3d_preview_canvas_box")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Floor shadow ellipse below the character
                            val floorY = size.height * 0.82f
                            val shadowCx = size.width / 2f
                            val shadowCy = floorY + 6f
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent),
                                    center = Offset(shadowCx, shadowCy),
                                    radius = 70f
                                ),
                                topLeft = Offset(shadowCx - 70f, shadowCy - 18f),
                                size = Size(140f, 36f)
                            )

                            // Render Skin via projected 3D pixels
                            val projectedList = Skin3DRenderer.buildProjectedPixels(
                                skinPixels = pixels,
                                format = activeProject.format,
                                modelType = activeProject.modelType,
                                pose = viewModel.previewPose.value,
                                animTime = activeTime,
                                pitch = pitch,
                                yaw = yaw,
                                zoom = zoom3D,
                                width = size.width,
                                height = size.height,
                                outerLayerVisibility = outerToggles
                            )

                            // Draw sorted projected pixels
                            for (p in projectedList) {
                                drawRect(
                                    color = Color(p.color),
                                    topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                                    size = Size(p.size + 0.35f, p.size + 0.35f) // slightly larger to prevent cracks
                                )

                                // Overlay wireframe grid cells on 3D pixels if enabled
                                if (show3DGrid) {
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.22f),
                                        topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                                        size = Size(p.size + 0.35f, p.size + 0.35f),
                                        style = Stroke(width = 0.5f)
                                    )
                                }
                            }
                        }

                        // Floating instructions hint
                        Text(
                            text = if (autoRotate) "Auto-rotating • Drag to orbit" else "Drag to rotate • Pinch to zoom",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // ── 2D PIXEL EDITOR ───────────────────────────────────────────────────
                    // Full-viewport canvas with pinch-to-zoom/pan and precision pixel drawing
                    var canvas2DScale by remember { mutableStateOf(1f) }
                    var canvas2DOffset by remember { mutableStateOf(Offset.Zero) }
                    var is2DPanMode by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Dark checkerboard backdrop (shows through transparent pixels)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cs = 14f
                            for (bx in 0 until (size.width / cs).toInt() + 1) {
                                for (by in 0 until (size.height / cs).toInt() + 1) {
                                    drawRect(
                                        color = if ((bx + by) % 2 == 0) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        topLeft = Offset(bx * cs, by * cs),
                                        size = Size(cs, cs)
                                    )
                                }
                            }
                        }

                        // Main editable canvas
                        val (texW, texH) = SkinTextureMapper.getDimensions(activeProject.format)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("drawing_grid_container")
                                .pointerInput(is2DPanMode) {
                                    if (is2DPanMode) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            canvas2DScale = (canvas2DScale * zoom).coerceIn(0.25f, 14f)
                                            canvas2DOffset += pan
                                        }
                                    }
                                }
                                .pointerInput(mapping, canvas2DScale, canvas2DOffset, is2DPanMode, activeTool) {
                                    if (!is2DPanMode) {
                                        fun toGrid(pos: Offset): Pair<Int, Int> {
                                            val base = minOf(size.width, size.height) * 0.82f
                                            val total = base * canvas2DScale
                                            val ox = size.width / 2f + canvas2DOffset.x - total / 2f
                                            val oy = size.height / 2f + canvas2DOffset.y - (total * mapping.height / mapping.width) / 2f
                                            val cellW = total / mapping.width
                                            val cellH = total / mapping.width  // square cells
                                            val u = ((pos.x - ox) / cellW).toInt().coerceIn(0, mapping.width - 1)
                                            val v = ((pos.y - oy) / cellH).toInt().coerceIn(0, mapping.height - 1)
                                            return Pair(u, v)
                                        }
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val (u, v) = toGrid(offset)
                                                if (activeTool == "BUCKET") viewModel.floodFillFace(u, v)
                                                else viewModel.paintPixel(u, v)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val (u, v) = toGrid(change.position)
                                                if (activeTool != "BUCKET") viewModel.paintPixel(u, v)
                                            }
                                        )
                                    }
                                }
                                .pointerInput(mapping, canvas2DScale, canvas2DOffset, is2DPanMode, activeTool) {
                                    if (!is2DPanMode) {
                                        fun toGrid(pos: Offset): Pair<Int, Int> {
                                            val base = minOf(size.width, size.height) * 0.82f
                                            val total = base * canvas2DScale
                                            val ox = size.width / 2f + canvas2DOffset.x - total / 2f
                                            val oy = size.height / 2f + canvas2DOffset.y - (total * mapping.height / mapping.width) / 2f
                                            val cellW = total / mapping.width
                                            val cellH = total / mapping.width
                                            val u = ((pos.x - ox) / cellW).toInt().coerceIn(0, mapping.width - 1)
                                            val v = ((pos.y - oy) / cellH).toInt().coerceIn(0, mapping.height - 1)
                                            return Pair(u, v)
                                        }
                                        detectTapGestures { offset ->
                                            val (u, v) = toGrid(offset)
                                            if (activeTool == "BUCKET") viewModel.floodFillFace(u, v)
                                            else viewModel.paintPixel(u, v)
                                        }
                                    }
                                }
                        ) {
                            val base = minOf(size.width, size.height) * 0.82f
                            val total = base * canvas2DScale
                            val cellW = total / mapping.width
                            val cellH = cellW  // square pixels
                            val gridH = cellH * mapping.height
                            val ox = size.width / 2f + canvas2DOffset.x - total / 2f
                            val oy = size.height / 2f + canvas2DOffset.y - gridH / 2f

                            // White checkerboard inside grid area (transparency indicator)
                            val checkPx = (cellW / 2f).coerceIn(2f, 10f)
                            val cols = (total / checkPx).toInt() + 1
                            val rows = (gridH / checkPx).toInt() + 1
                            for (cx2 in 0 until cols) {
                                for (cy2 in 0 until rows) {
                                    drawRect(
                                        color = if ((cx2 + cy2) % 2 == 0) Color.White else Color(0xFFD0D0D0),
                                        topLeft = Offset(ox + cx2 * checkPx, oy + cy2 * checkPx),
                                        size = Size(checkPx + 0.5f, checkPx + 0.5f)
                                    )
                                }
                            }

                            // Draw all pixels for the active face
                            if (mapping.x + mapping.width <= texW && mapping.y + mapping.height <= texH) {
                                for (dy in 0 until mapping.height) {
                                    for (dx in 0 until mapping.width) {
                                        val pixelColor = pixels[(mapping.y + dy) * texW + (mapping.x + dx)]
                                        val alpha = (pixelColor ushr 24) and 0xFF
                                        if (alpha > 0) {
                                            drawRect(
                                                color = Color(pixelColor),
                                                topLeft = Offset(ox + dx * cellW, oy + dy * cellH),
                                                size = Size(cellW + 0.5f, cellH + 0.5f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Mirror/symmetry centre line
                            if (mirrorMode) {
                                val midX = ox + total / 2f
                                drawLine(
                                    color = Color(0xFFFFD600).copy(alpha = 0.9f),
                                    start = Offset(midX, oy),
                                    end = Offset(midX, oy + gridH),
                                    strokeWidth = 2.5f
                                )
                                // Dashed visual: small ticks on mirror line
                                for (t in 0 until (gridH / 12).toInt()) {
                                    if (t % 2 == 0) {
                                        drawLine(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            start = Offset(midX, oy + t * 12f),
                                            end = Offset(midX, oy + t * 12f + 6f),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }

                            // Pixel grid lines (only when cell size > 4px to avoid clutter)
                            if (show2DGrid && cellW > 4f) {
                                for (i in 0..mapping.width) {
                                    val lx = ox + i * cellW
                                    drawLine(Color.Gray.copy(alpha = 0.45f), Offset(lx, oy), Offset(lx, oy + gridH), 0.7f)
                                }
                                for (j in 0..mapping.height) {
                                    val ly = oy + j * cellH
                                    drawLine(Color.Gray.copy(alpha = 0.45f), Offset(ox, ly), Offset(ox + total, ly), 0.7f)
                                }
                            }

                            // Grid border
                            drawRect(
                                color = Color.White.copy(alpha = 0.55f),
                                topLeft = Offset(ox, oy),
                                size = Size(total, gridH),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Top info bar overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${activePart.name.replace("_", " ")} · ${activeFace.name} · ${if (activeLayer == LayerType.INNER) "Base" else "Overlay"}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                val toolLabel = when (activeTool) {
                                    "BRUSH" -> "Brush (${brushSize}px)"
                                    "PENCIL" -> "Pencil (1px)"
                                    "ERASER" -> "Eraser (${brushSize}px)"
                                    "TRANSPARENT_BRUSH" -> "Transparent Brush"
                                    "BUCKET" -> "Paint Bucket"
                                    "EYEDROPPER" -> "Eyedropper / Pick"
                                    else -> activeTool
                                }
                                Text(
                                    text = if (mirrorMode) "⬡ MIRROR · $toolLabel" else toolLabel,
                                    color = if (mirrorMode) Color(0xFFFFD600) else Color.White.copy(alpha = 0.65f),
                                    fontSize = 10.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (activeTool in listOf("BRUSH", "ERASER")) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(1, 2, 3).forEach { sz ->
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(if (brushSize == sz) Color(0xFF1976D2) else Color.White.copy(alpha = 0.18f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                                    .clickable { viewModel.setBrushSize(sz) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(sz.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(activeColor))
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                            }
                        }

                        // Pan / Draw mode toggle + Reset zoom — bottom-left
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 72.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallFloatingButton(
                                icon = if (is2DPanMode) Icons.Default.Edit else Icons.Default.PanTool,
                                contentDescription = if (is2DPanMode) "Draw mode" else "Pan/Zoom mode",
                                onClick = { is2DPanMode = !is2DPanMode },
                                testTag = "pan_mode_toggle_button"
                            )
                            SmallFloatingButton(
                                icon = Icons.Default.CenterFocusStrong,
                                contentDescription = "Reset zoom",
                                onClick = { canvas2DScale = 1f; canvas2DOffset = Offset.Zero },
                                testTag = "reset_2d_zoom_button"
                            )
                        }
                    }
                }

                // --- 4. FLOATING TOOLBARS (Left & Right Side columns overlaid on the screen) ---

                // Left Column: Navigation, Tools, Layer
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Rotate Button (Circular arrow orbit)
                    FloatingToolbarButton(
                        icon = Icons.Default.Sync,
                        contentDescription = "Rotate Orbit",
                        onClick = {
                            if (selectedTab == 1) {
                                autoRotate = !autoRotate
                            } else {
                                // Cycle face target
                                val nextFace = FaceType.values()[(activeFace.ordinal + 1) % FaceType.values().size]
                                viewModel.setEditingTarget(activePart, nextFace, activeLayer)
                            }
                        },
                        isSelected = autoRotate && selectedTab == 1,
                        testTag = "auto_rotate_orbit_button"
                    )

                    // Eraser Tool Button
                    FloatingToolbarButton(
                        icon = Icons.Outlined.CleaningServices,
                        contentDescription = "Eraser Tool",
                        onClick = { viewModel.setTool("ERASER") },
                        isSelected = activeTool == "ERASER",
                        testTag = "tool_button_ERASER"
                    )

                    // Paint Bucket (Fill) Button
                    FloatingToolbarButton(
                        icon = Icons.Default.FormatColorFill,
                        contentDescription = "Bucket Fill Tool",
                        onClick = { viewModel.setTool("BUCKET") },
                        isSelected = activeTool == "BUCKET",
                        testTag = "tool_button_BUCKET"
                    )

                    // Eyedropper Button
                    FloatingToolbarButton(
                        icon = Icons.Default.Colorize,
                        contentDescription = "Eyedropper Tool",
                        onClick = { viewModel.setTool("EYEDROPPER") },
                        isSelected = activeTool == "EYEDROPPER",
                        testTag = "tool_button_EYEDROPPER"
                    )

                    // Pencil Tool (always 1px, precision drawing)
                    FloatingToolbarButton(
                        icon = Icons.Default.Create,
                        contentDescription = "Pencil Tool (1px)",
                        onClick = { viewModel.setTool("PENCIL") },
                        isSelected = activeTool == "PENCIL",
                        testTag = "tool_button_PENCIL"
                    )

                    // Transparent Brush (50% alpha strokes for layered shading)
                    FloatingToolbarButton(
                        icon = Icons.Default.WaterDrop,
                        contentDescription = "Transparent Brush",
                        onClick = { viewModel.setTool("TRANSPARENT_BRUSH") },
                        isSelected = activeTool == "TRANSPARENT_BRUSH",
                        testTag = "tool_button_TRANSPARENT_BRUSH"
                    )

                    // Mirror/Symmetry mode toggle (yellow when active)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(if (mirrorMode) Color(0xFFFF8F00) else Color(0xFF1976D2).copy(alpha = 0.85f))
                            .border(1.5.dp, if (mirrorMode) Color(0xFFFFD600) else Color.White, CircleShape)
                            .clickable { viewModel.toggleMirrorMode() }
                            .testTag("mirror_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flip, contentDescription = "Mirror Mode", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("MIRROR", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Undo & Redo Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallFloatingButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Action",
                            onClick = { viewModel.undo() },
                            enabled = viewModel.isUndoAvailable(),
                            testTag = "undo_button"
                        )
                        SmallFloatingButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo Action",
                            onClick = { viewModel.redo() },
                            enabled = viewModel.isRedoAvailable(),
                            testTag = "redo_button"
                        )
                    }

                    // Shirt/Jacket Accessory layer toggle
                    FloatingToolbarButton(
                        icon = Icons.Default.Checkroom,
                        contentDescription = "Outer Layer Toggle",
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.toggleLayer()
                            } else {
                                showOuterLayerOnly = !showOuterLayerOnly
                            }
                        },
                        isSelected = if (selectedTab == 0) activeLayer == LayerType.OUTER else showOuterLayerOnly,
                        testTag = "outer_layer_toggle_button"
                    )
                }

                // Right Column: Palette, Grid, Brush, Settings, 3D switch, Zooms
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Color Selection circle opening palette dialog
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { showColorPicker = true }
                            .testTag("color_picker_trigger_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.size(24.dp)) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFFEB3B)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF44336)))
                            }
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF4CAF50)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2196F3)))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(activeColor))
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }

                    // Grid Overlay toggle
                    FloatingToolbarButton(
                        icon = Icons.Default.GridOn,
                        contentDescription = "Grid Overlay Toggle",
                        onClick = {
                            if (selectedTab == 0) {
                                show2DGrid = !show2DGrid
                            } else {
                                show3DGrid = !show3DGrid
                            }
                        },
                        isSelected = if (selectedTab == 0) show2DGrid else show3DGrid,
                        testTag = "grid_overlay_toggle_button"
                    )

                    // Brush Tool (multi-pixel, respects brush size)
                    FloatingToolbarButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Brush Tool",
                        onClick = { viewModel.setTool("BRUSH") },
                        isSelected = activeTool == "BRUSH",
                        testTag = "tool_button_BRUSH"
                    )

                    // Settings Gear (Format/Skeletal configurations)
                    FloatingToolbarButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Skin Settings",
                        onClick = { showFormatDialog = true },
                        isSelected = false,
                        testTag = "skin_settings_button"
                    )

                    // 3D/2D Mode switcher
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2))
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { selectedTab = if (selectedTab == 0) 1 else 0 }
                            .testTag("mode_toggle_3d_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Switch Dimensions",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedTab == 0) "3D" else "2D",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Zoom In / Zoom Out Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallFloatingButton(
                            icon = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In Preview",
                            onClick = {
                                if (selectedTab == 1) {
                                    zoom3D = (zoom3D + 0.12f).coerceIn(0.5f, 2.0f)
                                }
                            },
                            testTag = "zoom_in_button"
                        )
                        SmallFloatingButton(
                            icon = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out Preview",
                            onClick = {
                                if (selectedTab == 1) {
                                    zoom3D = (zoom3D - 0.12f).coerceIn(0.5f, 2.0f)
                                }
                            },
                            testTag = "zoom_out_button"
                        )
                    }
                }

                // Body selection panel overlay — triggered from the bottom centre row
                if (showBodyPanel) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Body Part Selection",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBodyPanel = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                // Select Body Part Section
                                Text(
                                    text = "Select Part",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(BodyPart.values()) { part ->
                                        val isSel = activePart == part
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFF1976D2) else Color.White.copy(alpha = 0.15f))
                                                .clickable { viewModel.setEditingTarget(part, activeFace, activeLayer) }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = part.name.replace("_", " "),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // Select Face Section
                                Text(
                                    text = "Select Face",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(FaceType.values()) { face ->
                                        val isSel = activeFace == face
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFF388E3C) else Color.White.copy(alpha = 0.15f))
                                                .clickable { viewModel.setEditingTarget(activePart, face, activeLayer) }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = face.name,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                // Walk Loop switch toggle inside panel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3D Walk Loop Action",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = isAnimating,
                                        onCheckedChange = { viewModel.setAnimating3D(it) },
                                        modifier = Modifier.scale(0.72f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5b. Bottom centre action row — Body Part selector + Layer visibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Body / face selector trigger
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (showBodyPanel) Color(0xFF0D47A1) else Color(0xFF1976D2).copy(alpha = 0.85f))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { showBodyPanel = !showBodyPanel }
                        .testTag("body_panel_bottom_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Body Selector",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Outer-layer (jacket/hat) visibility toggle
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (showOuterLayerOnly) Color(0xFF1976D2).copy(alpha = 0.85f)
                            else Color.Gray.copy(alpha = 0.55f)
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { showOuterLayerOnly = !showOuterLayerOnly }
                        .testTag("layer_visibility_bottom_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showOuterLayerOnly) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle outer layer",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 5. Solid Red Bottom Action Bar — "Edit" in 3D view, "← 3D View" in 2D mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC62828))
                    .clickable { selectedTab = if (selectedTab == 1) 0 else 1 }
                    .padding(vertical = 13.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (selectedTab == 0) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (selectedTab == 1) "Edit" else "3D Preview",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    if (selectedTab == 1) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS & OVERLAYS ---

    // Floating Custom Color Picker overlay
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = activeColor,
            recentColors = recentColors,
            onColorSelected = { viewModel.setColor(it) },
            onDismiss = { showColorPicker = false }
        )
    }

    // Skin Configuration Format Dialog (Steve vs Alex proportions, resolution)
    if (showFormatDialog) {
        var tempFormat by remember { mutableStateOf(activeProject.format) }
        var tempModel by remember { mutableStateOf(activeProject.modelType) }

        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("Skin Layout Proportions", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Resize Format Resolution", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("64x32", "64x64", "128x128").forEach { format ->
                            FilterChip(
                                selected = tempFormat == format,
                                onClick = { tempFormat = format },
                                label = { Text(format) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Skeletal Figure Proportions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            onClick = { tempModel = "STEVE" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tempModel == "STEVE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Steve Model", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("4px standard arms", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            onClick = { tempModel = "ALEX" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tempModel == "ALEX") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Alex Model", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("3px skinny arms", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempFormat != activeProject.format) {
                            viewModel.setFormatType(tempFormat)
                        }
                        if (tempModel != activeProject.modelType) {
                            viewModel.setModelType(tempModel)
                        }
                        showFormatDialog = false
                    }
                ) {
                    Text("Apply Layout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Export Gallery Dialog
    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(48.dp)) },
            title = { Text("Export Completed!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your customized skin texture sheet has been saved to your local device Pictures gallery successfully.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You can now import this file into Minecraft Pocket Edition / Bedrock (via Profile > Change Classic Skin > Choose New Skin) to play!",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showExportSuccess = false }) {
                    Text("Awesome")
                }
            }
        )
    }

    // Import Skin Dialog — gallery (PNG/JPG) or file picker
    if (showImportOptions) {
        AlertDialog(
            onDismissRequest = { showImportOptions = false },
            icon = {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text("Import Skin", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose a skin PNG file from your device. The file should be a standard Minecraft skin (64×64, 64×32, or 128×128 pixels).",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            showImportOptions = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery / Photos")
                    }
                    OutlinedButton(
                        onClick = {
                            showImportOptions = false
                            galleryLauncher.launch("image/png")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("File Storage (PNG)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Custom UI component for floating column buttons
@Composable
fun FloatingToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (isSelected) Color(0xFF0D47A1) else if (enabled) Color(0xFF1976D2).copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.5f)
            )
            .border(1.5.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// Custom UI component for small row floating buttons
@Composable
fun SmallFloatingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (enabled) Color(0xFF1976D2).copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.5f)
            )
            .border(1.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// Custom Grid color selector popover
@Composable
fun ColorPickerDialog(
    initialColor: Int,
    recentColors: List<Int>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    val presetColors = listOf(
        0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(),
        0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(),
        0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFFF5722.toInt(),
        0xFF795548.toInt(), 0xFF9E9E9E.toInt(), 0xFF607D8B.toInt(), 0xFF000000.toInt(),
        0xFFFFFFFF.toInt(), 0xFF3E2723.toInt(), 0xFF004D40.toInt(), 0xFF1A237E.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Palette Color",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Color preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(selectedColor))
                            .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "ARGB HEX VALUE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("#%08X", selectedColor),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Color presets grid
                Text(
                    text = "Vibrant Swatches",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunks = presetColors.chunked(6)
                    chunks.forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { color ->
                                val isSelected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }

                // Recent colors
                if (recentColors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recents",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentColors.take(6).forEach { color ->
                            val isSelected = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                }
            ) {
                Text("Confirm Selection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
