package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skin_projects")
data class SkinProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val format: String = "64x64", // "64x32", "64x64", "128x128"
    val modelType: String = "STEVE", // "STEVE", "ALEX"
    val textureData: ByteArray, // PNG image bytes
    val folder: String = "My Skins",
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkinProject) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (format != other.format) return false
        if (modelType != other.modelType) return false
        if (!textureData.contentEquals(other.textureData)) return false
        if (folder != other.folder) return false
        if (isFavorite != other.isFavorite) return false
        if (lastUpdated != other.lastUpdated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + modelType.hashCode()
        result = 31 * result + textureData.contentHashCode()
        result = 31 * result + folder.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        return result
    }
}
