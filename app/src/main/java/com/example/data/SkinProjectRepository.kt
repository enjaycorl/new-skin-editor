package com.example.data

import kotlinx.coroutines.flow.Flow

class SkinProjectRepository(private val skinProjectDao: SkinProjectDao) {
    val allProjects: Flow<List<SkinProject>> = skinProjectDao.getAllProjects()
    val allFolders: Flow<List<String>> = skinProjectDao.getAllFolders()
    val favoriteProjects: Flow<List<SkinProject>> = skinProjectDao.getFavoriteProjects()

    suspend fun getProjectById(id: Int): SkinProject? {
        return skinProjectDao.getProjectById(id)
    }

    fun getProjectsInFolder(folder: String): Flow<List<SkinProject>> {
        return skinProjectDao.getProjectsInFolder(folder)
    }

    suspend fun insertProject(project: SkinProject): Long {
        return skinProjectDao.insertProject(project)
    }

    suspend fun deleteProject(project: SkinProject) {
        skinProjectDao.deleteProject(project)
    }

    suspend fun deleteProjectById(id: Int) {
        skinProjectDao.deleteProjectById(id)
    }
}
