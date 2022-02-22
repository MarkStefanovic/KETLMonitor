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
import kotlinx.coroutines.SupervisorJob
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
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
@ExperimentalMaterialApi
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
fun main() = application {
  val logger = Logger.getLogger("KETL Monitor")

  val handler = FileHandler("./error.log", 1048576L, 1, true).apply {
    level = Level.SEVERE
  }

  logger.addHandler(handler)

  logger.info("Starting KETL Monitor...")

  try {
    Class.forName("org.postgresql.Driver")
  } catch (ex: ClassNotFoundException) {
    println("Unable to load the class, org.postgresql.Driver. Terminating the program...")
    exitProcess(-1)
  }

  val state = rememberWindowState(
    width = 900.dp, // use Dp.Unspecified to auto-fit
    height = Dp.Unspecified,
    position = WindowPosition.Aligned(Alignment.TopStart),
  )

  val config = getConfig()

  val hikariConfig = HikariConfig().apply {
    jdbcUrl = config.pgURL
    username = config.pgUsername
    password = config.pgPassword
    maximumPoolSize = 1
  }

  val pgDataSource = HikariDataSource(hikariConfig)

  val pgJobResultRepo = PgJobResultRepo(
    ds = pgDataSource,
    schema = "ketl",
    showSQL = false,
  )

  val pgJobStatusRepo = PgJobStatusRepo(
    ds = pgDataSource,
    schema = "ketl",
    showSQL = false,
  )

  val pgJobLogRepo = PgJobLogRepo(
    ds = pgDataSource,
    schema = "ketl",
    showSQL = false,
  )

  val jobResultEvents = DefaultJobResultEvents()

  val jobResultStates = DefaultJobResultStates()

  val jobLogEvents = DefaultJobLogEvents()

  val jobLogStates = DefaultJobLogStates()

  val jobStatusEvents: JobStatusEvents = DefaultJobStatusEvents()

  val jobStatusStates: JobStatusStates = DefaultJobStatusStates()

  scope.launch {
    jobResultBloc(
      repo = pgJobResultRepo,
      events = jobResultEvents,
      states = jobResultStates,
      logger = logger,
    )

    jobLogBloc(
      repo = pgJobLogRepo,
      events = jobLogEvents,
      states = jobLogStates,
      maxEntriesToDisplay = 1000,
      logger = logger,
    )

    jobStatusBloc(
      states = jobStatusStates,
      repo = pgJobStatusRepo,
      events = jobStatusEvents,
      logger = logger,
    )

    refreshJobResultsEvery(events = jobResultEvents, duration = 1.minutes)

    refreshJobLogEvery(events = jobLogEvents, duration = 1.minutes)

    refreshJobStatusesEvery(events = jobStatusEvents, duration = 1.minutes)
  }

  Window(
    onCloseRequest = {
      scope.cancel()
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
          jobResultsStateFlow = jobResultStates.stream,
          jobResultsEvents = jobResultEvents,
          jobStatusStateFlow = jobStatusStates.stream,
          jobStatusEvents = jobStatusEvents,
          jobLogStateFlow = jobLogStates.stream,
          jobLogEvents = jobLogEvents,
        )
      }
    }
  }
}
