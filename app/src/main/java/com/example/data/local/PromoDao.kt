package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PromoProject
import kotlinx.coroutines.flow.Flow

@Dao
interface PromoDao {

    @Query("SELECT * FROM promo_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<PromoProject>>

    @Query("SELECT * FROM promo_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): PromoProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: PromoProject): Long

    @Update
    suspend fun updateProject(project: PromoProject)

    @Delete
    suspend fun deleteProject(project: PromoProject)

    @Query("DELETE FROM promo_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM promo_projects")
    suspend fun getProjectCount(): Int
}
