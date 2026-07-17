package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SkinProject
import com.example.data.SkinProjectRepository
import com.example.utils.BodyPart
import com.example.utils.EnvironmentType
import com.example.utils.FaceType
import com.example.utils.LayerType
import com.example.utils.ModelPose
import com.example.utils.SkinGenerator
import com.example.utils.SkinTextureMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Stack

class SkinCraftViewModel(private val repository: SkinProjectRepository) : ViewModel() {

    // All skins in DB
    val allProjects: StateFlow<List<SkinProject>> = repository.allItemsStateFlow()

    private val _currentProject = MutableStateFlow<SkinProject?>(null)
    val currentProject: StateFlow<SkinProject?> = _currentProject.asStateFlow()

    // Active in-memory pixels representing the open project
    private val _editorPixels = MutableStateFlow<IntArray>(IntArray(64 * 64))
    val editorPixels: StateFlow<IntArray> = _editorPixels.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = Stack<IntArray>()
    private val redoStack = Stack<IntArray>()

    // Editor Tools
    private val _selectedTool = MutableStateFlow("BRUSH") // BRUSH, ERASER, BUCKET, EYEDROPPER
    val selectedTool: StateFlow<String> = _selectedTool.asStateFlow()

    private val _activeColor = MutableStateFlow(0xFF4CAF50.toInt()) // Default Green
    val activeColor: StateFlow<Int> = _activeColor.asStateFlow()

    private val _recentColors = MutableStateFlow(
        listOf(
            0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
            0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(),
            0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(),
            0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFFF5722.toInt(),
            0xFF795548.toInt(), 0xFF9E9E9E.toInt(), 0xFF607D8B.toInt(), 0xFF000000.toInt()
        )
    )
    val recentColors: StateFlow<List<Int>> = _recentColors.asStateFlow()

    // Current face & part being edited in 2D View
    private val _activeBodyPart = MutableStateFlow(BodyPart.HEAD)
    val activeBodyPart: StateFlow<BodyPart> = _activeBodyPart.asStateFlow()

    private val _activeFace = MutableStateFlow(FaceType.FRONT)
    val activeFace: StateFlow<FaceType> = _activeFace.asStateFlow()

    private val _activeLayer = MutableStateFlow(LayerType.INNER)
    val activeLayer: StateFlow<LayerType> = _activeLayer.asStateFlow()

    // 3D Preview Configuration
    private val _previewPose = MutableStateFlow(ModelPose.STAND)
    val previewPose: StateFlow<ModelPose> = _previewPose.asStateFlow()

    private val _previewEnvironment = MutableStateFlow(EnvironmentType.GRASS)
    val previewEnvironment: StateFlow<EnvironmentType> = _previewEnvironment.asStateFlow()

    private val _is3DAnimating = MutableStateFlow(false)
    val is3DAnimating: StateFlow<Boolean> = _is3DAnimating.asStateFlow()

    // Mirror/Symmetry mode: also paints the horizontally mirrored pixel on the same face
    private val _mirrorMode = MutableStateFlow(false)
    val mirrorMode: StateFlow<Boolean> = _mirrorMode.asStateFlow()

    // Brush size in pixels (1-3)
    private val _brushSize = MutableStateFlow(1)
    val brushSize: StateFlow<Int> = _brushSize.asStateFlow()

    // General states
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    init {
        // Pre-populate empty DB with template skins if needed
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                if (list.isEmpty()) {
                    createDefaultTemplates()
                }
            }
        }
    }

    private fun SkinProjectRepository.allItemsStateFlow(): StateFlow<List<SkinProject>> {
        return this.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private suspend fun createDefaultTemplates() {
        withContext(Dispatchers.IO) {
            val templates = listOf(
                Pair("Hero Steve", "STEVE"),
                Pair("Explorer Alex", "ALEX"),
                Pair("Grass Creeper", "STEVE"),
                Pair("Royal Knight", "STEVE"),
                Pair("Ender Mage", "STEVE"),
                Pair("Cute Red Panda", "STEVE"),
                Pair("Cyber Boy", "ALEX"),
                Pair("Undead Zombie", "STEVE")
            )
            for (t in templates) {
                val bytes = SkinGenerator.generateSkinPng(t.first, "64x64", t.second)
                repository.insertProject(
                    SkinProject(
                        name = t.first,
                        format = "64x64",
                        modelType = t.second,
                        textureData = bytes,
                        folder = "Starter Templates"
                    )
                )
            }
        }
    }

    fun selectProject(project: SkinProject) {
        _currentProject.value = project
        undoStack.clear()
        redoStack.clear()

        // Load PNG bytes into raw ARGB IntArray
        val bytes = project.textureData
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        _editorPixels.value = pixels
        bitmap.recycle()

        // Reset to head front inner
        _activeBodyPart.value = BodyPart.HEAD
        _activeFace.value = FaceType.FRONT
        _activeLayer.value = LayerType.INNER
    }

    fun createNewProject(name: String, format: String, modelType: String, startTemplate: String = "Steve") {
        viewModelScope.launch {
            _isSaving.value = true
            val bytes = withContext(Dispatchers.IO) {
                SkinGenerator.generateSkinPng(startTemplate, format, modelType)
            }
            val newProj = SkinProject(
                name = name.ifEmpty { "New Skin" },
                format = format,
                modelType = modelType,
                textureData = bytes,
                folder = "My Skins"
            )
            val id = withContext(Dispatchers.IO) {
                repository.insertProject(newProj)
            }
            val createdProj = repository.getProjectById(id.toInt())
            if (createdProj != null) {
                selectProject(createdProj)
                showStatus("Created skin project: ${createdProj.name}")
            }
            _isSaving.value = false
        }
    }

    fun importSkinFromGallery(context: Context, uri: android.net.Uri, name: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val out = ByteArrayOutputStream()
                    inputStream?.copyTo(out)
                    inputStream?.close()
                    out.toByteArray()
                }

                // Verify it's a valid bitmap and get format
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    showStatus("Error: Invalid skin file")
                    _isSaving.value = false
                    return@launch
                }
                val format = when {
                    bitmap.width == 128 && bitmap.height == 128 -> "128x128"
                    bitmap.width == 64 && bitmap.height == 32 -> "64x32"
                    else -> "64x64" // default
                }
                bitmap.recycle()

                val newProj = SkinProject(
                    name = name.ifEmpty { "Imported Skin" },
                    format = format,
                    modelType = "STEVE", // default, user can toggle
                    textureData = bytes,
                    folder = "Imported"
                )

                val id = withContext(Dispatchers.IO) {
                    repository.insertProject(newProj)
                }
                val createdProj = repository.getProjectById(id.toInt())
                if (createdProj != null) {
                    selectProject(createdProj)
                    showStatus("Imported: ${createdProj.name}")
                }
            } catch (e: Exception) {
                showStatus("Import failed: ${e.localizedMessage}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun importPlayerSkin(username: String) {
        viewModelScope.launch {
            _isSaving.value = true
            showStatus("Searching for player: $username...")
            try {
                val skinBytes = withContext(Dispatchers.IO) {
                    // Use mc-heads.net as a highly robust proxy to fetch official Minecraft skins directly
                    val urlStr = "https://mc-heads.net/skin/$username"
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.requestMethod = "GET"
                    connection.connect()
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.readBytes()
                    } else {
                        null
                    }
                }

                if (skinBytes != null && skinBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.size)
                    if (bitmap != null) {
                        val format = when {
                            bitmap.width == 128 && bitmap.height == 128 -> "128x128"
                            bitmap.width == 64 && bitmap.height == 32 -> "64x32"
                            else -> "64x64"
                        }
                        bitmap.recycle()

                        val newProj = SkinProject(
                            name = "$username's Skin",
                            format = format,
                            modelType = "STEVE",
                            textureData = skinBytes,
                            folder = "Imported"
                        )
                        val id = withContext(Dispatchers.IO) {
                            repository.insertProject(newProj)
                        }
                        val createdProj = repository.getProjectById(id.toInt())
                        if (createdProj != null) {
                            selectProject(createdProj)
                            showStatus("Successfully imported $username's skin!")
                        }
                    } else {
                        showStatus("Failed to decode $username's skin image")
                    }
                } else {
                    showStatus("Could not find Minecraft skin for player: $username")
                }
            } catch (e: Exception) {
                showStatus("Network lookup failed. Check connection or username.")
            } finally {
                _isSaving.value = false
            }
        }
    }

    // Set configuration
    fun setTool(tool: String) {
        _selectedTool.value = tool
    }

    fun setColor(color: Int) {
        _activeColor.value = color
        val list = _recentColors.value.toMutableList()
        if (!list.contains(color)) {
            list.add(0, color)
            if (list.size > 20) list.removeAt(list.size - 1)
            _recentColors.value = list
        }
    }

    fun setEditingTarget(part: BodyPart, face: FaceType, layer: LayerType) {
        _activeBodyPart.value = part
        _activeFace.value = face
        _activeLayer.value = layer
    }

    fun toggleLayer() {
        _activeLayer.value = if (_activeLayer.value == LayerType.INNER) LayerType.OUTER else LayerType.INNER
    }

    fun setModelType(type: String) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val updated = proj.copy(modelType = type, lastUpdated = System.currentTimeMillis())
            _currentProject.value = updated
            withContext(Dispatchers.IO) { repository.insertProject(updated) }
        }
    }

    fun setFormatType(format: String) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val updatedBytes = withContext(Dispatchers.IO) {
                // Resize or regenerate texture
                val originalBitmap = BitmapFactory.decodeByteArray(proj.textureData, 0, proj.textureData.size)
                val (newW, newH) = SkinTextureMapper.getDimensions(format)
                val resized = Bitmap.createScaledBitmap(originalBitmap, newW, newH, false)
                val out = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.PNG, 100, out)
                originalBitmap.recycle()
                resized.recycle()
                out.toByteArray()
            }
            val updated = proj.copy(format = format, textureData = updatedBytes, lastUpdated = System.currentTimeMillis())
            _currentProject.value = updated
            selectProject(updated)
            _isSaving.value = false
            showStatus("Format changed to $format")
        }
    }

    fun setPreviewPose(pose: ModelPose) {
        _previewPose.value = pose
    }

    fun setPreviewEnvironment(env: EnvironmentType) {
        _previewEnvironment.value = env
    }

    fun setAnimating3D(anim: Boolean) {
        _is3DAnimating.value = anim
    }

    fun toggleMirrorMode() {
        _mirrorMode.value = !_mirrorMode.value
    }

    fun setBrushSize(size: Int) {
        _brushSize.value = size.coerceIn(1, 3)
    }

    // 2D Painting logic
    // Tools: BRUSH, PENCIL (always 1px), ERASER, TRANSPARENT_BRUSH, BUCKET, EYEDROPPER
    fun paintPixel(faceX: Int, faceY: Int) {
        val proj = _currentProject.value ?: return
        val pixels = _editorPixels.value.clone()

        val mapping = SkinTextureMapper.getScaledFaceMapping(
            _activeBodyPart.value,
            _activeLayer.value,
            _activeFace.value,
            proj.modelType,
            proj.format
        )

        if (faceX < 0 || faceX >= mapping.width || faceY < 0 || faceY >= mapping.height) return
        val (w, _) = SkinTextureMapper.getDimensions(proj.format)

        // Pick the new pixel color based on active tool
        val newPixel: Int = when (_selectedTool.value) {
            "BRUSH", "PENCIL" -> _activeColor.value
            "ERASER" -> 0 // fully transparent
            "TRANSPARENT_BRUSH" -> {
                // 50% alpha overlay of active color
                val base = _activeColor.value
                val a = ((base ushr 24) and 0xFF) / 2
                (base and 0x00FFFFFF) or (a shl 24)
            }
            "EYEDROPPER" -> {
                val idx = (mapping.y + faceY) * w + (mapping.x + faceX)
                if (idx in pixels.indices && pixels[idx] != 0) setColor(pixels[idx])
                return
            }
            else -> return
        }

        // Pencil is always 1px regardless of brushSize setting
        val effectiveBrushSize = if (_selectedTool.value == "PENCIL") 1 else _brushSize.value

        // Quick no-op check for 1px case
        if (effectiveBrushSize == 1) {
            val idx = (mapping.y + faceY) * w + (mapping.x + faceX)
            if (idx < 0 || idx >= pixels.size || pixels[idx] == newPixel) return
        }

        saveToUndo(pixels)

        // Paint a square of effectiveBrushSize pixels centred on the touch point
        fun stamp(originFaceX: Int, originFaceY: Int) {
            val half = effectiveBrushSize / 2
            for (dy in -half until effectiveBrushSize - half) {
                for (dx in -half until effectiveBrushSize - half) {
                    val px = (mapping.x + originFaceX + dx).coerceIn(mapping.x, mapping.x + mapping.width - 1)
                    val py = (mapping.y + originFaceY + dy).coerceIn(mapping.y, mapping.y + mapping.height - 1)
                    val idx = py * w + px
                    if (idx in pixels.indices) pixels[idx] = newPixel
                }
            }
        }

        stamp(faceX, faceY)

        // Mirror mode: also stamp the horizontally reflected pixel on this face
        if (_mirrorMode.value) {
            val mirrorFaceX = mapping.width - 1 - faceX
            stamp(mirrorFaceX, faceY)
        }

        _editorPixels.value = pixels
        triggerAutoSave()
    }

    fun floodFillFace(startFaceX: Int, startFaceY: Int) {
        val proj = _currentProject.value ?: return
        val pixels = _editorPixels.value.clone()

        val mapping = SkinTextureMapper.getScaledFaceMapping(
            _activeBodyPart.value,
            _activeLayer.value,
            _activeFace.value,
            proj.modelType,
            proj.format
        )

        if (startFaceX < 0 || startFaceX >= mapping.width || startFaceY < 0 || startFaceY >= mapping.height) return

        val (w, h) = SkinTextureMapper.getDimensions(proj.format)

        val targetColor = if (_selectedTool.value == "ERASER") 0 else _activeColor.value
        val startX = mapping.x + startFaceX
        val startY = mapping.y + startFaceY
        val startIdx = startY * w + startX
        val baseColor = pixels[startIdx]

        if (baseColor == targetColor) return

        saveToUndo(pixels)

        // Queue-based BFS Flood Fill restricted to the face mapping bounding box
        val queue = java.util.LinkedList<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))

        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            val idx = cy * w + cx
            if (pixels[idx] == baseColor) {
                pixels[idx] = targetColor

                // Add neighbors
                val neighbors = listOf(
                    Pair(cx + 1, cy),
                    Pair(cx - 1, cy),
                    Pair(cx, cy + 1),
                    Pair(cx, cy - 1)
                )

                for (n in neighbors) {
                    // Ensure neighbors are inside the 2D mapping bounds
                    if (n.first >= mapping.x && n.first < mapping.x + mapping.width &&
                        n.second >= mapping.y && n.second < mapping.y + mapping.height
                    ) {
                        val nIdx = n.second * w + n.first
                        if (pixels[nIdx] == baseColor) {
                            queue.add(n)
                        }
                    }
                }
            }
        }

        _editorPixels.value = pixels
        triggerAutoSave()
    }

    private fun saveToUndo(oldPixels: IntArray) {
        undoStack.push(oldPixels)
        redoStack.clear() // Clear redo stack on new action
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_editorPixels.value.clone())
            _editorPixels.value = undoStack.pop()
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_editorPixels.value.clone())
            _editorPixels.value = redoStack.pop()
            triggerAutoSave()
        }
    }

    fun isUndoAvailable(): Boolean = undoStack.isNotEmpty()
    fun isRedoAvailable(): Boolean = redoStack.isNotEmpty()

    // Autosave triggers database updates with debouncing
    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Debounce for 1 second
            saveActiveSkin()
        }
    }

    suspend fun saveActiveSkin() {
        val proj = _currentProject.value ?: return
        val pixels = _editorPixels.value
        val (w, h) = SkinTextureMapper.getDimensions(proj.format)

        val updatedBytes = withContext(Dispatchers.IO) {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }

        val updated = proj.copy(
            textureData = updatedBytes,
            lastUpdated = System.currentTimeMillis()
        )
        _currentProject.value = updated
        withContext(Dispatchers.IO) {
            repository.insertProject(updated)
        }
    }

    fun deleteProject(project: SkinProject) {
        viewModelScope.launch {
            _isSaving.value = true
            withContext(Dispatchers.IO) {
                repository.deleteProject(project)
            }
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
            }
            _isSaving.value = false
            showStatus("Deleted: ${project.name}")
        }
    }

    fun toggleFavorite(project: SkinProject) {
        viewModelScope.launch {
            val updated = project.copy(isFavorite = !project.isFavorite)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = updated
            }
            withContext(Dispatchers.IO) {
                repository.insertProject(updated)
            }
        }
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_statusMessage.value == msg) {
                _statusMessage.value = null
            }
        }
    }
}

class SkinCraftViewModelFactory(private val repository: SkinProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkinCraftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SkinCraftViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
