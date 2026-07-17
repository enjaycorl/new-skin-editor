package com.example.utils

import android.graphics.Color
import com.example.data.SkinProject
import kotlin.math.cos
import kotlin.math.sin

enum class ModelPose { STAND, WALK, RUN, WAVE, ZOMBIE }
enum class EnvironmentType { GRASS, NETHER, END, VOID }

data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun rotateX(angle: Float): Point3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Point3D(x, y * cosA - z * sinA, y * sinA + z * cosA)
    }

    fun rotateY(angle: Float): Point3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Point3D(x * cosA + z * sinA, y, -x * sinA + z * cosA)
    }

    fun rotateZ(angle: Float): Point3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Point3D(x * cosA - y * sinA, x * sinA + y * cosA, z)
    }

    fun add(other: Point3D): Point3D {
        return Point3D(x + other.x, y + other.y, z + other.z)
    }
}

data class ProjectedPixel(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
    val depth: Float
)

object Skin3DRenderer {

    /**
     * Builds a list of projected 2D pixels for rendering on the Canvas.
     */
    fun buildProjectedPixels(
        skinPixels: IntArray,
        format: String,
        modelType: String,
        pose: ModelPose,
        animTime: Float, // Oscillating variable for walking/running
        pitch: Float, // Yaw/pitch from user swipe controls
        yaw: Float,
        zoom: Float,
        width: Float,
        height: Float,
        layerVisibility: Map<BodyPart, Boolean> = BodyPart.values().associateWith { true },
        outerLayerVisibility: Map<BodyPart, Boolean> = BodyPart.values().associateWith { true }
    ): List<ProjectedPixel> {
        val (skinW, skinH) = SkinTextureMapper.getDimensions(format)
        val isAlex = modelType.uppercase() == "ALEX"

        val projectedList = mutableListOf<ProjectedPixel>()

        // Base Scale for 3D coordinates to Screen Pixels
        val scale = zoom * (width.coerceAtMost(height) / 45f)

        // Local joint rotations based on pose
        var headRotX = 0f
        var headRotY = 0f
        var headRotZ = 0f

        var leftArmRotX = 0f
        var leftArmRotY = 0f
        var leftArmRotZ = 0f

        var rightArmRotX = 0f
        var rightArmRotY = 0f
        var rightArmRotZ = 0f

        var leftLegRotX = 0f
        var leftLegRotY = 0f
        var leftLegRotZ = 0f

        var rightLegRotX = 0f
        var rightLegRotY = 0f
        var rightLegRotZ = 0f

        var bodyTiltX = 0f

        when (pose) {
            ModelPose.STAND -> {
                // Symmetrical stand, no rotation
            }
            ModelPose.WALK -> {
                val cycle = sin(animTime)
                leftArmRotX = cycle * 0.6f
                rightArmRotX = -cycle * 0.6f
                leftLegRotX = -cycle * 0.5f
                rightLegRotX = cycle * 0.5f
            }
            ModelPose.RUN -> {
                val cycle = sin(animTime * 1.5f)
                leftArmRotX = cycle * 1.0f
                leftArmRotY = 0.1f
                rightArmRotX = -cycle * 1.0f
                rightArmRotY = -0.1f
                leftLegRotX = -cycle * 0.8f
                rightLegRotX = cycle * 0.8f
                bodyTiltX = 0.2f // Tilt body slightly forward when running
                headRotX = -0.1f
            }
            ModelPose.WAVE -> {
                rightArmRotX = -2.5f // Raised arm
                rightArmRotZ = sin(animTime * 3f) * 0.3f - 0.2f // Wave swing
                leftArmRotX = 0.1f
            }
            ModelPose.ZOMBIE -> {
                leftArmRotX = -1.57f // Arms pointing straight forward
                rightArmRotX = -1.57f
                leftArmRotY = 0.1f
                rightArmRotY = -0.1f
                // Slight head tilt
                headRotX = 0.1f
                headRotY = 0.1f
            }
        }

        // Helper to project a single 3D point in model space to screen space
        fun project(
            point: Point3D,
            jointOffset: Point3D,
            localRotX: Float,
            localRotY: Float,
            localRotZ: Float
        ): Pair<Point3D, Point3D> {
            // 1. Rotate relative to Joint Pivot
            var rotated = Point3D(point.x, point.y, point.z)
            if (localRotX != 0f) rotated = rotated.rotateX(localRotX)
            if (localRotY != 0f) rotated = rotated.rotateY(localRotY)
            if (localRotZ != 0f) rotated = rotated.rotateZ(localRotZ)

            // 2. Translate from Joint Pivot to Model Space
            var modelPoint = rotated.add(jointOffset)

            // 3. Apply Torso Tilt (if any)
            if (bodyTiltX != 0f && jointOffset.y != 0f) { // don't tilt legs, only upper body
                modelPoint = modelPoint.rotateX(bodyTiltX)
            }

            // 4. Apply Global Camera Rotations
            val camRotated = modelPoint.rotateX(pitch).rotateY(yaw)

            return Pair(modelPoint, camRotated)
        }

        // Helper to safely read color from the 2D texture sheet
        fun getTextureColor(tx: Int, ty: Int): Int {
            if (tx < 0 || tx >= skinW || ty < 0 || ty >= skinH) return 0
            return skinPixels[ty * skinW + tx]
        }

        // General function to build a 3D box's surface points
        fun addBoxPixels(
            part: BodyPart,
            layer: LayerType,
            sizeX: Int,
            sizeY: Int,
            sizeZ: Int,
            jointOffset: Point3D,
            localRotX: Float,
            localRotY: Float,
            localRotZ: Float,
            expand: Float = 0f // Outer layer is expanded outward
        ) {
            // Check visibility toggles
            if (layer == LayerType.INNER && layerVisibility[part] == false) return
            if (layer == LayerType.OUTER && outerLayerVisibility[part] == false) return

            val halfX = sizeX / 2f
            val halfY = sizeY / 2f
            val halfZ = sizeZ / 2f

            // Loop over all 6 faces
            for (face in FaceType.values()) {
                val mapping = SkinTextureMapper.getScaledFaceMapping(part, layer, face, modelType, format)
                if (mapping.width == 0 || mapping.height == 0) continue

                // Loop over the pixel grid on this face
                for (v in 0 until mapping.height) {
                    for (u in 0 until mapping.width) {
                        val color = getTextureColor(mapping.x + u, mapping.y + v)
                        if ((color ushr 24) == 0) continue // Skip fully transparent pixels

                        // Minecraft-style directional shading — each face gets a fixed
                        // brightness based on its world-space normal direction, giving the
                        // model depth without expensive per-pixel lighting.
                        val shadeFactor = when (face) {
                            FaceType.TOP    -> 1.00f
                            FaceType.FRONT  -> 0.85f
                            FaceType.BACK   -> 0.70f
                            FaceType.LEFT   -> 0.75f
                            FaceType.RIGHT  -> 0.65f
                            FaceType.BOTTOM -> 0.50f
                        }
                        val a = (color ushr 24) and 0xFF
                        val r = (((color ushr 16) and 0xFF) * shadeFactor).toInt().coerceIn(0, 255)
                        val g = (((color ushr 8)  and 0xFF) * shadeFactor).toInt().coerceIn(0, 255)
                        val b = ((color and 0xFF)           * shadeFactor).toInt().coerceIn(0, 255)
                        val shadedColor = (a shl 24) or (r shl 16) or (g shl 8) or b

                        // Map 2D face coordinate (u, v) to 3D local offset relative to joint
                        // We scale the offset to match the size of the box
                        val pctU = (u + 0.5f) / mapping.width
                        val pctV = (v + 0.5f) / mapping.height

                        // Calculate 3D offset relative to box center
                        var lx = 0f
                        var ly = 0f
                        var lz = 0f

                        // Determine the local 3D position based on which face we are rendering
                        when (face) {
                            FaceType.TOP -> {
                                lx = -halfX + pctU * sizeX
                                ly = halfY + expand
                                lz = halfZ - pctV * sizeZ
                            }
                            FaceType.BOTTOM -> {
                                lx = -halfX + pctU * sizeX
                                ly = -halfY - expand
                                lz = -halfZ + pctV * sizeZ
                            }
                            FaceType.FRONT -> {
                                lx = -halfX + pctU * sizeX
                                ly = halfY - pctV * sizeY
                                lz = halfZ + expand
                            }
                            FaceType.BACK -> {
                                lx = halfX - pctU * sizeX
                                ly = halfY - pctV * sizeY
                                lz = -halfZ - expand
                            }
                            FaceType.RIGHT -> {
                                lx = -halfX - expand
                                ly = halfY - pctV * sizeY
                                lz = halfZ - pctU * sizeZ
                            }
                            FaceType.LEFT -> {
                                lx = halfX + expand
                                ly = halfY - pctV * sizeY
                                lz = -halfZ + pctU * sizeZ
                            }
                        }

                        // Apply expansions for Outer layers
                        if (expand > 0) {
                            if (lx > 0) lx += expand else if (lx < 0) lx -= expand
                            if (ly > 0) ly += expand else if (ly < 0) ly -= expand
                            if (lz > 0) lz += expand else if (lz < 0) lz -= expand
                        }

                        // Vertex relative to local joint
                        // Note: joint pivots are placed strategically:
                        // Head: rotates around bottom center (0, -halfY, 0) relative to head center, i.e., local offset is (lx, ly + halfY, lz)
                        val localPoint = when (part) {
                            BodyPart.HEAD -> Point3D(lx, ly + halfY, lz)
                            BodyPart.TORSO -> Point3D(lx, ly, lz)
                            else -> Point3D(lx, ly + halfY, lz) // limbs rotate around top joint
                        }

                        // Rotate and Project
                        val (_, projected) = project(
                            localPoint,
                            jointOffset,
                            localRotX,
                            localRotY,
                            localRotZ
                        )

                        // Calculate screen coordinates
                        val screenX = width / 2f + projected.x * scale
                        val screenY = height / 2f - projected.y * scale

                        // Calculate pixel size based on depth to give a subtle 3D perspective sizing
                        val voxel3DSize = sizeX.toFloat() / mapping.width
                        val pointScale = voxel3DSize * scale * 1.15f
                        val pixelSize = pointScale * (1f + projected.z * 0.003f).coerceIn(0.5f, 50f)

                        projectedList.add(
                            ProjectedPixel(
                                x = screenX,
                                y = screenY,
                                size = pixelSize,
                                color = shadedColor,
                                depth = projected.z
                            )
                        )
                    }
                }
            }
        }

        // Render entire body back-to-front or collect all pixels and sort globally.
        // Sorting globally resolves all overlaps, which is the perfect solution.

        val expandAmt = 0.45f

        // 1. Torso: size 8 x 12 x 4. Center at (0, 0, 0). Joint offset is (0, 0, 0).
        addBoxPixels(BodyPart.TORSO, LayerType.INNER, 8, 12, 4, Point3D(0f, 0f, 0f), 0f, 0f, 0f)
        addBoxPixels(BodyPart.TORSO, LayerType.OUTER, 8, 12, 4, Point3D(0f, 0f, 0f), 0f, 0f, 0f, expandAmt)

        // 2. Head: size 8 x 8 x 8. Joint is at (0, 6, 0).
        addBoxPixels(BodyPart.HEAD, LayerType.INNER, 8, 8, 8, Point3D(0f, 6f, 0f), headRotX, headRotY, headRotZ)
        addBoxPixels(BodyPart.HEAD, LayerType.OUTER, 8, 8, 8, Point3D(0f, 6f, 0f), headRotX, headRotY, headRotZ, expandAmt)

        // 3. Right Arm: size 4 x 12 x 4 (Steve) or 3 x 12 x 4 (Alex). Joint is at (6, 6, 0).
        val armW = if (isAlex) 3 else 4
        val rArmJoint = Point3D(4f + armW / 2f, 6f, 0f)
        addBoxPixels(BodyPart.RIGHT_ARM, LayerType.INNER, armW, 12, 4, rArmJoint, rightArmRotX, rightArmRotY, rightArmRotZ)
        addBoxPixels(BodyPart.RIGHT_ARM, LayerType.OUTER, armW, 12, 4, rArmJoint, rightArmRotX, rightArmRotY, rightArmRotZ, expandAmt)

        // 4. Left Arm: size 4 x 12 x 4 (Steve) or 3 x 12 x 4 (Alex). Joint is at (-6, 6, 0).
        val lArmJoint = Point3D(-4f - armW / 2f, 6f, 0f)
        addBoxPixels(BodyPart.LEFT_ARM, LayerType.INNER, armW, 12, 4, lArmJoint, leftArmRotX, leftArmRotY, leftArmRotZ)
        addBoxPixels(BodyPart.LEFT_ARM, LayerType.OUTER, armW, 12, 4, lArmJoint, leftArmRotX, leftArmRotY, leftArmRotZ, expandAmt)

        // 5. Right Leg: size 4 x 12 x 4. Joint is at (2, -6, 0).
        addBoxPixels(BodyPart.RIGHT_LEG, LayerType.INNER, 4, 12, 4, Point3D(2f, -6f, 0f), rightLegRotX, rightLegRotY, rightLegRotZ)
        addBoxPixels(BodyPart.RIGHT_LEG, LayerType.OUTER, 4, 12, 4, Point3D(2f, -6f, 0f), rightLegRotX, rightLegRotY, rightLegRotZ, expandAmt)

        // 6. Left Leg: size 4 x 12 x 4. Joint is at (-2, -6, 0).
        addBoxPixels(BodyPart.LEFT_LEG, LayerType.INNER, 4, 12, 4, Point3D(-2f, -6f, 0f), leftLegRotX, leftLegRotY, leftLegRotZ)
        addBoxPixels(BodyPart.LEFT_LEG, LayerType.OUTER, 4, 12, 4, Point3D(-2f, -6f, 0f), leftLegRotX, leftLegRotY, leftLegRotZ, expandAmt)

        // Sort all projected pixels by depth (back-to-front)
        projectedList.sortBy { it.depth }

        return projectedList
    }

    /**
     * Environment background colors
     */
    fun getEnvironmentColors(type: EnvironmentType): List<Int> {
        return when (type) {
            EnvironmentType.GRASS -> listOf(0xFF87CEEB.toInt(), 0xFF556B2F.toInt(), 0xFF8FBC8F.toInt()) // Sky blue, dark olive, light green
            EnvironmentType.NETHER -> listOf(0xFF2B0505.toInt(), 0xFF721C1C.toInt(), 0xFF140202.toInt()) // Deep blood red, netherrack red, black
            EnvironmentType.END -> listOf(0xFF0F001A.toInt(), 0xFF3D0C5A.toInt(), 0xFFEEDD82.toInt()) // Dark purple, bright void purple, endstone yellow
            EnvironmentType.VOID -> listOf(0xFF020205.toInt(), 0xFF12121E.toInt(), 0xFF4A4A6A.toInt()) // Deep void, cosmic slate, nebula dust
        }
    }
}
