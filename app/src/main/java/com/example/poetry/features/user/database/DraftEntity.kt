package com.example.poetry.features.user.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String?,
    val createdAt: Long,      // 创建时间戳
    val updatedAt: Long       // 更新时间戳
)