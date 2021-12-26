package domain

interface JobStatusRepo {
  suspend fun getLatestStatuses(): List<JobStatus>
}
