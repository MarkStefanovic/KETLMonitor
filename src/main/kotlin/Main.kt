@file:Suppress("JSON_FORMAT_REDUNDANT")

import adapter.PgJobLogRepo
import adapter.PgJobResultRepo
import adapter.PgJobStatusRepo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.material.darkColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import domain.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import presentation.MainView
import presentation.log.bloc.DefaultJobLogEvents
import presentation.log.bloc.DefaultJobLogStates
import presentation.log.bloc.jobLogBloc
import presentation.log.bloc.refreshJobLogEvery
import presentation.result.bloc.DefaultJobResultEvents
import presentation.result.bloc.DefaultJobResultStates
import presentation.result.bloc.jobResultBloc
import presentation.result.bloc.refreshJobResultsEvery
import presentation.status.bloc.DefaultJobStatusEvents
import presentation.status.bloc.DefaultJobStatusStates
import presentation.status.bloc.JobStatusEvents
import presentation.status.bloc.JobStatusStates
import presentation.status.bloc.jobStatusBloc
import presentation.status.bloc.refreshJobStatusesEvery
import java.io.File
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

fun getConfig(): Config {
  val configFile = File("./config.json")

  if (!configFile.exists()) {
    throw Exception("${configFile.path} was not found.")
  }

  val configJsonText = configFile.readText()

  return Json {
    ignoreUnknownKeys = true
  }.decodeFromString(configJsonText)
}

@FlowPreview
@ExperimentalTime
@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
object Services {
  private val scope = CoroutineScope(Job() + Dispatchers.Default)

  private val logger: Logger = Logger.getLogger("KETL Monitor").apply {
    val fileHandler = FileHandler("./error.log", 1048576L, 1, true).apply {
      level = Level.SEVERE
    }

    val consoleHandler = ConsoleHandler().apply {
      level = Level.ALL
    }

    addHandler(fileHandler)

    addHandler(consoleHandler)
  }

  val jobResultEvents = DefaultJobResultEvents(
    scope = scope,
    logger = logger,
  )

  val jobResultStates = DefaultJobResultStates()

  val jobLogEvents = DefaultJobLogEvents(
    scope = scope,
    logger = logger,
  )

  val jobLogStates = DefaultJobLogStates()

  val jobStatusEvents: JobStatusEvents = DefaultJobStatusEvents(
    scope = scope,
    logger = logger,
  )

  val jobStatusStates: JobStatusStates = DefaultJobStatusStates()

  init {
    logger.info("Starting KETL Monitor...")

    try {
      Class.forName("org.postgresql.Driver")
    } catch (ex: ClassNotFoundException) {
      println("Unable to load the class, org.postgresql.Driver. Terminating the program...")
      exitProcess(-1)
    }

    val jsonConfig = getConfig()

    val hikariConfig = HikariConfig().apply {
      jdbcUrl = jsonConfig.pgURL
      username = jsonConfig.pgUsername
      password = jsonConfig.pgPassword
      maximumPoolSize = 3
    }

    val ds = HikariDataSource(hikariConfig)

    val jobResultRepo = PgJobResultRepo(
      ds = ds,
      schema = "ketl",
      showSQL = false,
    )

    val jobStatusRepo = PgJobStatusRepo(
      ds = ds,
      schema = "ketl",
      showSQL = false,
    )

    val jobLogRepo = PgJobLogRepo(
      ds = ds,
      schema = "ketl",
      showSQL = false,
    )

    scope.launch {
      jobResultBloc(
        repo = jobResultRepo,
        events = jobResultEvents,
        states = jobResultStates,
        logger = logger,
      )

      jobLogBloc(
        repo = jobLogRepo,
        events = jobLogEvents,
        states = jobLogStates,
        maxEntriesToDisplay = 1000,
        logger = logger,
      )

      jobStatusBloc(
        repo = jobStatusRepo,
        states = jobStatusStates,
        events = jobStatusEvents,
        logger = logger,
      )

      refreshJobResultsEvery(events = jobResultEvents, duration = 1.minutes)

      refreshJobLogEvery(events = jobLogEvents, duration = 1.minutes)

      refreshJobStatusesEvery(events = jobStatusEvents, duration = 1.minutes)
    }
  }

  fun stop() {
    logger.info("Stopping KETL Monitor...")

    scope.cancel()
  }
}

@FlowPreview
@ExperimentalTime
@DelicateCoroutinesApi
@ExperimentalMaterialApi
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
fun main() = application {
  val state = rememberWindowState(
    width = 900.dp, // use Dp.Unspecified to auto-fit
    height = Dp.Unspecified,
    position = WindowPosition.Aligned(Alignment.TopStart),
  )

  Window(
    onCloseRequest = {
      Services.stop()
      exitApplication()
      exitProcess(0)
    },
    state = state,
    title = "KETL Monitor",
    resizable = true,
  ) {
    MaterialTheme(colors = darkColors()) {
      Surface(
        color = MaterialTheme.colors.surface,
        contentColor = contentColorFor(MaterialTheme.colors.surface),
        modifier = Modifier.fillMaxSize(),
      ) {
        MainView(
          jobResultsStateFlow = Services.jobResultStates.stream,
          jobResultsEvents = Services.jobResultEvents,
          jobStatusStateFlow = Services.jobStatusStates.stream,
          jobStatusEvents = Services.jobStatusEvents,
          jobLogStateFlow = Services.jobLogStates.stream,
          jobLogEvents = Services.jobLogEvents,
        )
      }
    }
  }
}
