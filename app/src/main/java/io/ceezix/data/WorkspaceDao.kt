package io.ceezix.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_files ORDER BY isFolder DESC, path ASC")
    fun getAllFiles(): Flow<List<WorkspaceFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: WorkspaceFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<WorkspaceFile>)

    @Update
    suspend fun updateFile(file: WorkspaceFile)

    @Query("UPDATE workspace_files SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFileContent(id: Int, content: String, updatedAt: Long)

    @Query("SELECT * FROM workspace_files WHERE id = :id")
    suspend fun getFileById(id: Int): WorkspaceFile?

    @Query("SELECT * FROM workspace_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): WorkspaceFile?

    @Delete
    suspend fun deleteFile(file: WorkspaceFile)

    @Query("DELETE FROM workspace_files")
    suspend fun clearWorkspace()

    // --- GIT VERSION CONTROL COMPONENT QUERIES ---
    @Query("SELECT * FROM git_commits ORDER BY timestamp DESC")
    fun getAllCommits(): Flow<List<GitCommitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommit(commit: GitCommitEntity)

    @Query("SELECT * FROM git_commits WHERE hash = :hash LIMIT 1")
    suspend fun getCommitByHash(hash: String): GitCommitEntity?

    @Query("DELETE FROM git_commits")
    suspend fun clearGitCommits()
}
