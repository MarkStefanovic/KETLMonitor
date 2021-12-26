import domain.Config
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

fun getTestConfig(): Config {
  val resourceDir: Path = Paths.get("src", "test", "resources", "config.json")

  val configFile: File = resourceDir.toFile()

  val jsonString = configFile.readText()

  return Json {
    ignoreUnknownKeys = true
  }.decodeFromString(jsonString)
}
