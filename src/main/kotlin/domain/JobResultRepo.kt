package domain

interface JobResultRepo {
  suspend fun getLatestResults(
    jobNameStartsWith: String?,
    resultFilter: ResultFilter,
  ): List<JobResult>

  suspend fun getResultsForJob(
    selectedJob: String,
    resultFilter: ResultFilter,
  ): List<JobResult>
}
