package io.ceezix.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "git_commits")
data class GitCommitEntity(
    @PrimaryKey val hash: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val filesJson: String // Contains JSON string: list of files snapshots
)

data class WorkspaceFileSnapshot(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val content: String
)
