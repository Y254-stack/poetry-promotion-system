package com.example.poetry.features.user.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraftById(id: Long): DraftEntity?

    @Insert
    suspend fun insert(draft: DraftEntity): Long

    @Update
    suspend fun update(draft: DraftEntity)

    @Delete
    suspend fun delete(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
}