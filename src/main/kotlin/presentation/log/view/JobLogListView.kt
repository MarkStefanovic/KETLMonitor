package presentation.log.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import presentation.log.bloc.JobLogEvents
import presentation.log.bloc.JobLogState
import presentation.shared.EnumDropdown
import presentation.shared.abbreviatedTimestampFormat

@Composable
@FlowPreview
@ExperimentalMaterialApi
@ExperimentalFoundationApi
fun JobLogListView(
  stateFlow: StateFlow<JobLogState>,
  events: JobLogEvents,
) {
  val state by stateFlow.collectAsState()

  val listState = rememberLazyListState()

  val coroutineScope = rememberCoroutineScope()

  val showScrollToTopButton by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex > 0
    }
  }

  var jobNameFilter by remember { mutableStateOf(state.filter) }

  var logLevelFilter by remember { mutableStateOf(state.logLevel) }

  LaunchedEffect("jobNameFilter") {
    snapshotFlow {
      jobNameFilter
    }
      .distinctUntilChanged()
      .debounce(1000)
      .collect {
        events.setFilter(
          jobNamePrefix = it,
          logLevel = logLevelFilter,
        )
      }
  }

//  LaunchedEffect("logLevelFilter") {
//    snapshotFlow {
//      logLevelFilter
//    }
//    .distinctUntilChanged()
//    .debounce(1000)
//    .collect {
//      events.setFilter(
//        jobNamePrefix = jobNameFilter,
//        logLevel = it,
//      )
//    }
//  }

//  println("JobLogListView.state: $state")

  Column(
    modifier = Modifier.padding(6.dp)
  ) {
    Row {
      AnimatedVisibility(visible = showScrollToTopButton) {
        IconButton(
          onClick = {
            coroutineScope.launch {
              listState.animateScrollToItem(index = 0)
            }
          }
        ) {
          Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Scroll to Top",
          )
        }
      }

      Button(
        onClick = events::refresh,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
        border = BorderStroke(width = 1.dp, color = Color.White),
      ) {
        Text(
          text = "Refresh",
          fontWeight = FontWeight.ExtraBold, color = Color.LightGray,
        )
      }

      if (state.latestRefresh != null) {
        Spacer(Modifier.weight(1f))

        Text(
          text = "Last Refresh: ${abbreviatedTimestampFormat.format(state.latestRefresh)}",
          modifier = Modifier.align(Alignment.CenterVertically),
        )
      }
    }

    Spacer(Modifier.height(6.dp))

    Row {
      TextField(
        value = jobNameFilter,
        onValueChange = { txt ->
          jobNameFilter = txt
        },
        label = { Text("Job", modifier = Modifier.fillMaxHeight()) },
        maxLines = 1,
        modifier = Modifier.weight(1f).height(65.dp),
//        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.width(10.dp))

      EnumDropdown(
        label = "Log Level",
        value = logLevelFilter,
        onValueChange = {
          logLevelFilter = it

          events.setFilter(
            jobNamePrefix = jobNameFilter,
            logLevel = it,
          )
        },
        modifier = Modifier.width(150.dp)
      )
    }

    when (val st = state) {
      JobLogState.Initial -> {
        println("${javaClass.simpleName} initialized")
      }
      is JobLogState.Loaded, is JobLogState.Loading -> {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 2.dp),
          state = listState,
        ) {
          items(items = st.logEntries) { logEntry ->
            JobLogListViewItem(logEntry = logEntry)
          }
        }

        Spacer(Modifier.weight(1f))

        Text("Status: ${state.status}")
      }
      is JobLogState.Error -> {
        Text(st.errorMessage)
      }
    }
  }
}
