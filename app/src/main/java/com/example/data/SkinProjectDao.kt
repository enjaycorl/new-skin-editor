package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkinProjectDao {
    @Query("SELECT * FROM skin_projects ORDER BY lastUpdated DESC")
    fun getAllProjects(): Flow<List<SkinProject>>

    @Query("SELECT * FROM skin_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): SkinProject?

    @Query("SELECT * FROM skin_projects WHERE folder = :folder ORDER BY lastUpdated DESC")
    fun getProjectsInFolder(folder: String): Flow<List<SkinProject>>

    @Query("SELECT DISTINCT folder FROM skin_projects")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM skin_projects WHERE isFavorite = 1 ORDER BY lastUpdated DESC")
    fun getFavoriteProjects(): Flow<List<SkinProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SkinProject): Long

    @Delete
    suspend fun deleteProject(project: SkinProject)

    @Query("DELETE FROM skin_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}
