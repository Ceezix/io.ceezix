package io.ceezix.data

import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(private val workspaceDao: WorkspaceDao) {
    val allFiles: Flow<List<WorkspaceFile>> = workspaceDao.getAllFiles()

    suspend fun insertFile(file: WorkspaceFile): Long {
        return workspaceDao.insertFile(file)
    }

    suspend fun insertFiles(files: List<WorkspaceFile>) {
        workspaceDao.insertFiles(files)
    }

    suspend fun updateFile(file: WorkspaceFile) {
        workspaceDao.updateFile(file)
    }

    suspend fun updateFileContent(id: Int, content: String) {
        workspaceDao.updateFileContent(id, content, System.currentTimeMillis())
    }

    suspend fun getFileById(id: Int): WorkspaceFile? {
        return workspaceDao.getFileById(id)
    }

    suspend fun getFileByPath(path: String): WorkspaceFile? {
        return workspaceDao.getFileByPath(path)
    }

    suspend fun deleteFile(file: WorkspaceFile) {
        workspaceDao.deleteFile(file)
    }

    suspend fun clearWorkspace() {
        workspaceDao.clearWorkspace()
    }

    // --- GIT VERSION CONTROL COMPONENT ACTIONS ---
    val allCommits: Flow<List<GitCommitEntity>> = workspaceDao.getAllCommits()

    suspend fun insertCommit(commit: GitCommitEntity) {
        workspaceDao.insertCommit(commit)
    }

    suspend fun getCommitByHash(hash: String): GitCommitEntity? {
        return workspaceDao.getCommitByHash(hash)
    }

    suspend fun clearGitCommits() {
        workspaceDao.clearGitCommits()
    }
}
