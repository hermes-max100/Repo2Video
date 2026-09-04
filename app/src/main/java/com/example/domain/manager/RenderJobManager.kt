package com.example.domain.manager

import com.example.data.local.RenderJobDao
import com.example.data.model.RenderJobEntity
import com.example.data.model.RenderJobStatus
import com.example.data.model.RenderJobType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RenderJobManager(private val renderJobDao: RenderJobDao) {

    fun getJobsForProject(projectId: Long): Flow<List<RenderJobEntity>> {
        return renderJobDao.getJobsForProject(projectId)
    }

    fun getActiveJobs(): Flow<List<RenderJobEntity>> {
        return renderJobDao.getActiveJobs()
    }

    suspend fun createAndStartJob(
        projectId: Long,
        type: RenderJobType,
        initialStep: String = "INITIALIZED"
    ): RenderJobEntity {
        val job = RenderJobEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            jobType = type.name,
            status = RenderJobStatus.PROCESSING.name,
            progressPercent = 5,
            checkpointStep = initialStep,
            isResumable = true,
            startedAt = System.currentTimeMillis()
        )
        renderJobDao.insertJob(job)
        return job
    }

    suspend fun updateCheckpoint(jobId: String, progress: Int, stepName: String) {
        renderJobDao.updateProgress(
            id = jobId,
            status = RenderJobStatus.PROCESSING.name,
            progress = progress.coerceIn(0, 99),
            checkpoint = stepName
        )
    }

    suspend fun completeJob(jobId: String, outputUri: String) {
        renderJobDao.markCompleted(
            id = jobId,
            outputUri = outputUri,
            completedAt = System.currentTimeMillis()
        )
    }

    suspend fun failJob(jobId: String, errorMessage: String) {
        renderJobDao.markFailed(jobId, errorMessage)
    }

    suspend fun cancelJob(jobId: String) {
        val job = renderJobDao.getJobById(jobId) ?: return
        renderJobDao.updateJob(
            job.copy(
                status = RenderJobStatus.CANCELLED.name,
                errorMessage = "Cancelled by user"
            )
        )
    }

    suspend fun retryJob(jobId: String): RenderJobEntity? {
        val job = renderJobDao.getJobById(jobId) ?: return null
        val retried = job.copy(
            status = RenderJobStatus.PROCESSING.name,
            progressPercent = 10,
            checkpointStep = "RESTARTED",
            errorMessage = null,
            retryCount = job.retryCount + 1,
            startedAt = System.currentTimeMillis()
        )
        renderJobDao.updateJob(retried)
        return retried
    }

    suspend fun deleteJob(jobId: String) {
        renderJobDao.deleteJobById(jobId)
    }
}
