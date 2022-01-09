package domain

enum class LogLevel {
  Any,
  Debug,
  Error,
  Info,
  Warning;

  companion object {
    fun fromString(level: String): LogLevel =
      when (level) {
        "debug" -> Debug
        "error" -> Error
        "info" -> Info
        "warning" -> Warning
        else -> error("Unrecognized log level: $level")
      }
  }
}
