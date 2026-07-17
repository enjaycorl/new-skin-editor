package com.example.utils

import com.example.data.SkinProject

enum class BodyPart { HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }
enum class LayerType { INNER, OUTER }
enum class FaceType { TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK }

data class FaceMapping(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMirrored: Boolean = false
)

object SkinTextureMapper {

    /**
     * Gets the pixel coordinates on the flat skin texture sheet for a given body part face.
     * Dimensions are based on a 64x64 skin size. They can be scaled for 128x128.
         */
    fun getFaceMapping(
        bodyPart: BodyPart,
        layer: LayerType,
        face: FaceType,
        modelType: String = "STEVE",
        format: String = "64x64"
    ): FaceMapping {
        val isAlex = modelType.uppercase() == "ALEX"
        val isClassic = format == "64x32"

        // In 64x32 format:
        // - Outer layer does NOT exist except for HEAD.
        // - LEFT_ARM and LEFT_LEG do not have independent textures (they mirror RIGHT_ARM and RIGHT_LEG).
        if (isClassic) {
            if (layer == LayerType.OUTER && bodyPart != BodyPart.HEAD) {
                // Return a dummy empty coordinate mapping or map to some empty space
                return FaceMapping(0, 0, 0, 0)
            }
            if (bodyPart == BodyPart.LEFT_ARM) {
                return getFaceMapping(BodyPart.RIGHT_ARM, layer, face, modelType, "64x64").copy(isMirrored = true)
            }
            if (bodyPart == BodyPart.LEFT_LEG) {
                return getFaceMapping(BodyPart.RIGHT_LEG, layer, face, modelType, "64x64").copy(isMirrored = true)
            }
        }

        return when (bodyPart) {
            BodyPart.HEAD -> {
                val base = if (layer == LayerType.INNER) Pair(0, 0) else Pair(32, 0)
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 8, base.second + 0, 8, 8)
                    FaceType.BOTTOM -> FaceMapping(base.first + 16, base.second + 0, 8, 8)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 8, 8, 8)
                    FaceType.FRONT -> FaceMapping(base.first + 8, base.second + 8, 8, 8)
                    FaceType.LEFT -> FaceMapping(base.first + 16, base.second + 8, 8, 8)
                    FaceType.BACK -> FaceMapping(base.first + 24, base.second + 8, 8, 8)
                }
            }

            BodyPart.TORSO -> {
                val base = if (layer == LayerType.INNER) Pair(16, 16) else Pair(16, 32)
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 4, base.second + 0, 8, 4)
                    FaceType.BOTTOM -> FaceMapping(base.first + 12, base.second + 0, 8, 4)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 4, 4, 12)
                    FaceType.FRONT -> FaceMapping(base.first + 4, base.second + 4, 8, 12)
                    FaceType.LEFT -> FaceMapping(base.first + 12, base.second + 4, 4, 12)
                    FaceType.BACK -> FaceMapping(base.first + 16, base.second + 4, 8, 12)
                }
            }

            BodyPart.RIGHT_LEG -> {
                val base = if (layer == LayerType.INNER) Pair(0, 16) else Pair(0, 32)
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 4, base.second + 0, 4, 4)
                    FaceType.BOTTOM -> FaceMapping(base.first + 8, base.second + 0, 4, 4)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 4, 4, 12)
                    FaceType.FRONT -> FaceMapping(base.first + 4, base.second + 4, 4, 12)
                    FaceType.LEFT -> FaceMapping(base.first + 8, base.second + 4, 4, 12)
                    FaceType.BACK -> FaceMapping(base.first + 12, base.second + 4, 4, 12)
                }
            }

            BodyPart.LEFT_LEG -> {
                val base = if (layer == LayerType.INNER) Pair(16, 48) else Pair(0, 48)
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 4, base.second + 0, 4, 4)
                    FaceType.BOTTOM -> FaceMapping(base.first + 8, base.second + 0, 4, 4)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 4, 4, 12)
                    FaceType.FRONT -> FaceMapping(base.first + 4, base.second + 4, 4, 12)
                    FaceType.LEFT -> FaceMapping(base.first + 8, base.second + 4, 4, 12)
                    FaceType.BACK -> FaceMapping(base.first + 12, base.second + 4, 4, 12)
                }
            }

            BodyPart.RIGHT_ARM -> {
                val base = if (layer == LayerType.INNER) Pair(40, 16) else Pair(40, 32)
                val armWidth = if (isAlex) 3 else 4
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 4, base.second + 0, armWidth, 4)
                    FaceType.BOTTOM -> FaceMapping(base.first + 4 + armWidth, base.second + 0, armWidth, 4)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 4, 4, 12)
                    FaceType.FRONT -> FaceMapping(base.first + 4, base.second + 4, armWidth, 12)
                    FaceType.LEFT -> FaceMapping(base.first + 4 + armWidth, base.second + 4, 4, 12)
                    FaceType.BACK -> FaceMapping(base.first + 8 + armWidth, base.second + 4, armWidth, 12)
                }
            }

            BodyPart.LEFT_ARM -> {
                val base = if (layer == LayerType.INNER) Pair(32, 48) else Pair(48, 48)
                val armWidth = if (isAlex) 3 else 4
                when (face) {
                    FaceType.TOP -> FaceMapping(base.first + 4, base.second + 0, armWidth, 4)
                    FaceType.BOTTOM -> FaceMapping(base.first + 4 + armWidth, base.second + 0, armWidth, 4)
                    FaceType.RIGHT -> FaceMapping(base.first + 0, base.second + 4, 4, 12)
                    FaceType.FRONT -> FaceMapping(base.first + 4, base.second + 4, armWidth, 12)
                    FaceType.LEFT -> FaceMapping(base.first + 4 + armWidth, base.second + 4, 4, 12)
                    FaceType.BACK -> FaceMapping(base.first + 8 + armWidth, base.second + 4, armWidth, 12)
                }
            }
        }
    }

    /**
     * Given skin format, gets actual dimensions (width, height) on texture sheet
     */
    fun getDimensions(format: String): Pair<Int, Int> {
        return when (format) {
            "64x32" -> Pair(64, 32)
            "128x128" -> Pair(128, 128)
            else -> Pair(64, 64)
        }
    }

    /**
     * Scale mapping for HD (128x128) skins
     */
    fun getScaledFaceMapping(
        bodyPart: BodyPart,
        layer: LayerType,
        face: FaceType,
        modelType: String = "STEVE",
        format: String = "64x64"
    ): FaceMapping {
        val mapping = getFaceMapping(bodyPart, layer, face, modelType, if (format == "128x128") "64x64" else format)
        if (format == "128x128") {
            return FaceMapping(
                x = mapping.x * 2,
                y = mapping.y * 2,
                width = mapping.width * 2,
                height = mapping.height * 2,
                isMirrored = mapping.isMirrored
            )
        }
        return mapping
    }
}
