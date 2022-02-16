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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import presentation.MainView
import presentation.log.bloc.DefaultJobLogEvents
import presentation.log.bloc.JobLogBloc
import presentation.result.bloc.DefaultJobResultEvents
import presentation.result.bloc.JobResultBloc
import presentation.status.bloc.DefaultJobStatusEvents
import presentation.status.bloc.JobStatusBloc
import presentation.status.bloc.JobStatusEvents
import java.io.File
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

  val mainScope = MainScope()

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

  val jobResultBloc = JobResultBloc(
    repo = pgJobResultRepo,
    events = jobResultEvents,
    logger = logger,
    dispatcher = Dispatchers.Default,
  )

  mainScope.launch {
    jobResultBloc.start()
  }

  mainScope.launch {
    jobResultBloc.autorefreshEvery(1.minutes)
  }

  val jobLogEvents = DefaultJobLogEvents()

  val jobLogBloc = JobLogBloc(
    repo = pgJobLogRepo,
    events = jobLogEvents,
    maxEntriesToDisplay = 1000,
    logger = logger,
    dispatcher = Dispatchers.Default,
  )

  mainScope.launch {
    jobLogBloc.start()
  }

  mainScope.launch {
    jobLogBloc.autorefreshEvery(1.minutes)
  }

  val jobStatusEvents: JobStatusEvents = DefaultJobStatusEvents()

  val jobStatusBloc = JobStatusBloc(
    repo = pgJobStatusRepo,
    events = jobStatusEvents,
    logger = logger,
    dispatcher = Dispatchers.Default,
  )

  mainScope.launch {
    jobStatusBloc.start()
  }

  mainScope.launch {
    jobStatusBloc.autorefreshEvery(1.minutes)
  }

  Window(
    onCloseRequest = ::exitApplication,
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
          jobResultsStateFlow = jobResultBloc.state,
          jobResultsEvents = jobResultEvents,
          jobStatusStateFlow = jobStatusBloc.state,
          jobStatusEvents = jobStatusEvents,
          jobLogStateFlow = jobLogBloc.state,
          jobLogEvents = jobLogEvents,
        )
      }
    }
  }
}
