package domain

interface JobResultRepo {
  suspend fun getLatestResults(): List<JobResult>
}
