package io.ceezix.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_files")
data class WorkspaceFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String, // relative path from "/" root of workspace
    val isFolder: Boolean,
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
