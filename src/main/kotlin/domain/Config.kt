package domain

import kotlinx.serialization.Serializable

@Serializable
data class Config(
  val pgURL: String,
  val pgUsername: String,
  val pgPassword: String,
) {
  override fun toString(): String =
    """
      |Config [
      |  pgURL: $pgURL
      |  pgUsername: $pgUsername
      |  pgPassword: ...
      |]
    """.trimMargin()
}
