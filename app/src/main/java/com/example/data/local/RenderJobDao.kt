package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RenderJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RenderJobDao {

    @Query("SELECT * FROM render_jobs ORDER BY startedAt DESC")
    fun getAllJobs(): Flow<List<RenderJobEntity>>

    @Query("SELECT * FROM render_jobs WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun getJobsForProject(projectId: Long): Flow<List<RenderJobEntity>>

    @Query("SELECT * FROM render_jobs WHERE status IN ('QUEUED', 'PROCESSING') ORDER BY startedAt ASC")
    fun getActiveJobs(): Flow<List<RenderJobEntity>>

    @Query("SELECT * FROM render_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: String): RenderJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: RenderJobEntity)

    @Update
    suspend fun updateJob(job: RenderJobEntity)

    @Query("UPDATE render_jobs SET status = :status, progressPercent = :progress, checkpointStep = :checkpoint WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int, checkpoint: String)

    @Query("UPDATE render_jobs SET status = 'COMPLETED', progressPercent = 100, outputUri = :outputUri, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, outputUri: String, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE render_jobs SET status = 'FAILED', errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("DELETE FROM render_jobs WHERE id = :id")
    suspend fun deleteJobById(id: String)

    @Query("DELETE FROM render_jobs WHERE projectId = :projectId")
    suspend fun deleteJobsForProject(projectId: Long)
}
