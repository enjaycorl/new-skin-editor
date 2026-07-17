package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.random.Random

object SkinGenerator {

    /**
     * Generates a skin texture as a PNG ByteArray.
     */
    fun generateSkinPng(templateName: String, format: String = "64x64", modelType: String = "STEVE"): ByteArray {
        val (w, h) = SkinTextureMapper.getDimensions(format)
        val pixels = IntArray(w * h) { 0 } // Transparent initially

        val random = Random(templateName.hashCode())

        // Basic helper to draw a face with Minecraft-style noise
        fun fillFace(
            part: BodyPart,
            layer: LayerType,
            face: FaceType,
            baseColor: Int,
            noiseRange: Int = 15,
            customPainter: ((x: Int, y: Int, color: Int) -> Int)? = null
        ) {
            val mapping = SkinTextureMapper.getScaledFaceMapping(part, layer, face, modelType, format)
            if (mapping.width == 0 || mapping.height == 0) return

            for (dy in 0 until mapping.height) {
                for (dx in 0 until mapping.width) {
                    val targetX = mapping.x + dx
                    val targetY = mapping.y + dy
                    if (targetX < 0 || targetX >= w || targetY < 0 || targetY >= h) continue

                    val noise = if (noiseRange > 0) random.nextInt(-noiseRange, noiseRange) else 0
                    
                    val r = ((baseColor ushr 16) and 0xFF).coerceIn(0, 255)
                    val g = ((baseColor ushr 8) and 0xFF).coerceIn(0, 255)
                    val b = (baseColor and 0xFF).coerceIn(0, 255)
                    val a = ((baseColor ushr 24) and 0xFF)

                    val finalR = (r + noise).coerceIn(0, 255)
                    val finalG = (g + noise).coerceIn(0, 255)
                    val finalB = (b + noise).coerceIn(0, 255)
                    
                    val pixelColor = (a ushr 24) or (finalR shl 16) or (finalG shl 8) or finalB or (a shl 24)

                    val finalColor = if (customPainter != null) {
                        customPainter(dx, dy, pixelColor)
                    } else {
                        pixelColor
                    }

                    pixels[targetY * w + targetX] = finalColor
                }
            }
        }

        val nameLower = templateName.lowercase()
        when {
            nameLower.contains("steve") || nameLower.contains("blank") -> {
                // Peach Skin, Blue Shirt, Purple Pants
                val skinColor = 0xFFFCD5B5.toInt()
                val hairColor = 0xFF4A2F17.toInt()
                val eyeColor = 0xFF2A55B2.toInt()
                val noseColor = 0xFFC68E65.toInt()
                val shirtColor = 0xFF26B1B1.toInt()
                val pantsColor = 0xFF5C33A3.toInt()
                val shoeColor = 0xFF6B6B6B.toInt()

                // Draw Head
                for (f in FaceType.values()) {
                    if (f == FaceType.TOP || f == FaceType.BOTTOM) {
                        fillFace(BodyPart.HEAD, LayerType.INNER, f, hairColor, 10)
                    } else {
                        fillFace(BodyPart.HEAD, LayerType.INNER, f, skinColor, 10) { dx, dy, color ->
                            if (f == FaceType.FRONT) {
                                when {
                                    // Hair top part
                                    dy < 3 -> hairColor
                                    dy == 3 && (dx == 0 || dx == 7) -> hairColor
                                    // Eyes
                                    dy == 4 && (dx == 1 || dx == 2 || dx == 5 || dx == 6) -> {
                                        if (dx == 1 || dx == 5) 0xFFFFFFFF.toInt() else eyeColor
                                    }
                                    // Nose/Mouth
                                    dy == 5 && (dx == 3 || dx == 4) -> noseColor
                                    dy == 6 && (dx in 2..5) -> 0xFF9E5C41.toInt() // Beard/mouth
                                    else -> color
                                }
                            } else if (f == FaceType.BACK || f == FaceType.LEFT || f == FaceType.RIGHT) {
                                // Hair goes down back and sides
                                if (dy < 5) hairColor else color
                            } else color
                        }
                    }
                }

                // Torso (Shirt)
                for (f in FaceType.values()) {
                    fillFace(BodyPart.TORSO, LayerType.INNER, f, shirtColor, 12)
                }

                // Arms
                for (part in listOf(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, shirtColor, 10) { _, dy, color ->
                            // Sleeve length: shirt for top 4 pixels, skin below
                            if (dy >= 4 && f != FaceType.TOP) skinColor else color
                        }
                    }
                }

                // Legs
                for (part in listOf(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, pantsColor, 10) { _, dy, color ->
                            // Boots at the bottom
                            if (dy >= 10 && f != FaceType.TOP) shoeColor else color
                        }
                    }
                }
            }

            nameLower.contains("alex") -> {
                // Ginger hair, Green shirt, Brown pants, Green eyes
                val skinColor = 0xFFFFDFC4.toInt()
                val hairColor = 0xFFD36829.toInt()
                val eyeColor = 0xFF4A894B.toInt()
                val shirtColor = 0xFF4B6E4A.toInt()
                val pantsColor = 0xFF5C4033.toInt()
                val shoeColor = 0xFF4A4A4A.toInt()

                // Head
                for (f in FaceType.values()) {
                    if (f == FaceType.TOP) {
                        fillFace(BodyPart.HEAD, LayerType.INNER, f, hairColor, 10)
                    } else {
                        fillFace(BodyPart.HEAD, LayerType.INNER, f, skinColor, 10) { dx, dy, color ->
                            if (f == FaceType.FRONT) {
                                when {
                                    dy < 3 -> hairColor
                                    dy == 3 && (dx == 0 || dx == 1 || dx == 6 || dx == 7) -> hairColor
                                    dy == 4 && (dx == 1 || dx == 2 || dx == 5 || dx == 6) -> {
                                        if (dx == 1 || dx == 5) 0xFFFFFFFF.toInt() else eyeColor
                                    }
                                    dy == 5 && (dx == 2 || dx == 3 || dx == 4 || dx == 5) -> 0xFFE09A8F.toInt() // Lips
                                    else -> color
                                }
                            } else if (f == FaceType.BACK || f == FaceType.LEFT || f == FaceType.RIGHT) {
                                if (dy < 6) hairColor else color
                            } else color
                        }
                    }
                }

                // Torso (Green Shirt)
                for (f in FaceType.values()) {
                    fillFace(BodyPart.TORSO, LayerType.INNER, f, shirtColor, 10)
                }

                // Slim Arms
                for (part in listOf(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, shirtColor, 10) { _, dy, color ->
                            if (dy >= 4 && f != FaceType.TOP) skinColor else color
                        }
                    }
                }

                // Legs
                for (part in listOf(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, pantsColor, 10) { _, dy, color ->
                            if (dy >= 10 && f != FaceType.TOP) shoeColor else color
                        }
                    }
                }
            }

            nameLower.contains("creeper") -> {
                // Dark green and light green noise everywhere, signature black face
                val greenCreeper = 0xFF47A047.toInt()
                val blackCreeper = 0xFF1C1C1C.toInt()

                // Color everything green creeper noise
                for (part in BodyPart.values()) {
                    for (l in LayerType.values()) {
                        if (l == LayerType.OUTER && part != BodyPart.HEAD) continue
                        for (f in FaceType.values()) {
                            fillFace(part, l, f, greenCreeper, 30)
                        }
                    }
                }

                // Draw Creeper Face on Head FRONT
                fillFace(BodyPart.HEAD, LayerType.INNER, FaceType.FRONT, greenCreeper, 20) { dx, dy, color ->
                    val isEye = (dy in 2..3 && (dx in 1..2 || dx in 5..6))
                    val isMouth = (dy == 4 && dx in 2..5) || 
                                  (dy == 5 && (dx in 1..6 && dx != 1 && dx != 6)) || 
                                  (dy in 6..7 && (dx == 2 || dx == 5))
                    when {
                        isEye || isMouth -> blackCreeper
                        else -> color
                    }
                }
            }

            nameLower.contains("zombie") -> {
                // Green skin, Blue shirt, Purple trousers
                val skinColor = 0xFF4E8E56.toInt()
                val shirtColor = 0xFF2A8FA1.toInt()
                val pantsColor = 0xFF4D3F75.toInt()

                // Head
                for (f in FaceType.values()) {
                    fillFace(BodyPart.HEAD, LayerType.INNER, f, skinColor, 15) { dx, dy, color ->
                        if (f == FaceType.FRONT && dy == 4 && (dx == 1 || dx == 2 || dx == 5 || dx == 6)) {
                            if (dx == 1 || dx == 5) 0xFF000000.toInt() else 0xFF2D2D2D.toInt()
                        } else color
                    }
                }

                // Torso
                for (f in FaceType.values()) {
                    fillFace(BodyPart.TORSO, LayerType.INNER, f, shirtColor, 12)
                }

                // Arms (Zombies have green arms)
                for (part in listOf(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, skinColor, 15)
                    }
                }

                // Legs
                for (part in listOf(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, pantsColor, 10)
                    }
                }
            }

            nameLower.contains("enderman") -> {
                // Black/Charcoal, Purple Eyes
                val blackColor = 0xFF161616.toInt()
                val purpleEyeColor = 0xFFCC33FF.toInt()

                for (part in BodyPart.values()) {
                    for (l in LayerType.values()) {
                        if (l == LayerType.OUTER && part != BodyPart.HEAD) continue
                        for (f in FaceType.values()) {
                            fillFace(part, l, f, blackColor, 10)
                        }
                    }
                }

                // Purple eyes on head front
                fillFace(BodyPart.HEAD, LayerType.INNER, FaceType.FRONT, blackColor, 5) { dx, dy, color ->
                    if (dy == 4 && (dx == 1 || dx == 2 || dx == 5 || dx == 6)) {
                        purpleEyeColor
                    } else color
                }
            }

            nameLower.contains("knight") -> {
                // Metallic grey armor, red trim
                val armorColor = 0xFF8A9597.toInt()
                val darkMetal = 0xFF4F5557.toInt()
                val goldColor = 0xFFD4AF37.toInt()
                val redCape = 0xFFB22222.toInt()

                for (part in BodyPart.values()) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, armorColor, 12)
                    }
                }

                // Head visor
                fillFace(BodyPart.HEAD, LayerType.INNER, FaceType.FRONT, armorColor, 5) { dx, dy, color ->
                    if (dy == 4 && dx in 1..6) darkMetal
                    else if (dy == 3 && dx in 3..4) goldColor
                    else color
                }

                // Red cape on torso back
                fillFace(BodyPart.TORSO, LayerType.INNER, FaceType.BACK, redCape, 10)
            }

            nameLower.contains("panda") -> {
                // Red Panda: Orange/Red, White/Cream face accents, Dark grey body/limbs
                val redFur = 0xFFC85A17.toInt()
                val darkFur = 0xFF2B2B2B.toInt()
                val creamColor = 0xFFFFF8DC.toInt()

                // Head
                for (f in FaceType.values()) {
                    fillFace(BodyPart.HEAD, LayerType.INNER, f, redFur, 15) { dx, dy, color ->
                        if (f == FaceType.FRONT) {
                            when {
                                dy >= 5 && (dx == 1 || dx == 2 || dx == 5 || dx == 6) -> creamColor
                                dy == 4 && (dx == 2 || dx == 5) -> darkFur // Eyes
                                dy == 5 && (dx == 3 || dx == 4) -> darkFur // Nose
                                else -> color
                            }
                        } else color
                    }
                }

                // Torso
                for (f in FaceType.values()) {
                    fillFace(BodyPart.TORSO, LayerType.INNER, f, darkFur, 10)
                }

                // Limbs
                for (part in listOf(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, darkFur, 10)
                    }
                }
            }

            else -> {
                // Default: Anime Hoodie Style
                val paleSkin = 0xFFFFE4E1.toInt()
                val hairColor = 0xFF2F4F4F.toInt()
                val hoodieColor = 0xFF6A5ACD.toInt() // Slate Blue Hoodie
                val shoeColor = 0xFFFFFFFF.toInt()

                // Head
                for (f in FaceType.values()) {
                    fillFace(BodyPart.HEAD, LayerType.INNER, f, paleSkin, 5) { dx, dy, color ->
                        if (dy < 4) hairColor
                        else if (f == FaceType.FRONT && dy == 4 && (dx == 2 || dx == 5)) 0xFF4682B4.toInt() // Blue eyes
                        else color
                    }
                }

                // Torso & Outer Hoodie
                for (f in FaceType.values()) {
                    fillFace(BodyPart.TORSO, LayerType.INNER, f, hoodieColor, 10)
                    fillFace(BodyPart.TORSO, LayerType.OUTER, f, hoodieColor, 10) { dx, dy, color ->
                        // White drawstrings on hoodie front outer layer
                        if (f == FaceType.FRONT && dy in 2..5 && (dx == 2 || dx == 5)) {
                            0xFFFFFFFF.toInt()
                        } else color
                    }
                }

                // Arms
                for (part in listOf(BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, hoodieColor, 10) { _, dy, color ->
                            if (dy >= 8 && f != FaceType.TOP) paleSkin else color
                        }
                    }
                }

                // Legs
                for (part in listOf(BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG)) {
                    for (f in FaceType.values()) {
                        fillFace(part, LayerType.INNER, f, 0xFF333333.toInt(), 10) { _, dy, color ->
                            if (dy >= 10 && f != FaceType.TOP) shoeColor else color
                        }
                    }
                }
            }
        }

        // Convert the raw ARGB pixels to a standard PNG Bitmap
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }
}
