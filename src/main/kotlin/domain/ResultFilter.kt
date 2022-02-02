package domain

enum class ResultFilter {
  All,
  Cancelled,
  Failed,
  Skipped,
  Successful;

  val dbName: String
    get() = when (this) {
      All -> ""
      Cancelled -> "cancelled"
      Failed -> "failed"
      Skipped -> "skipped"
      Successful -> "successful"
    }

  companion object {
    fun fromString(result: String): ResultFilter =
      when (result) {
        "all" -> All
        "cancelled" -> Cancelled
        "failed" -> Failed
        "skipped" -> Skipped
        "successful" -> Successful
        else -> error("Unrecognized result: '$result'; expected either 'all', 'cancelled', 'failed', 'skipped', or 'successful'.")
      }
  }
}
