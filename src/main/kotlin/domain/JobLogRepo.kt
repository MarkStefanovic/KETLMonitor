package domain

interface JobLogRepo {
  fun getLatestEntries(n: Int): List<JobLogEntry>

  fun getLatestEntriesForJobLike(jobNamePrefix: String, n: Int): List<JobLogEntry>
}
