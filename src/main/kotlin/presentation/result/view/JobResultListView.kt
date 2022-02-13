package presentation.result.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.ResultFilter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import presentation.result.bloc.JobResultEvents
import presentation.result.bloc.JobResultState
import presentation.shared.EnumDropdown
import presentation.shared.JobNameDropdown
import presentation.shared.abbreviatedTimestampFormat

@Composable
@ExperimentalMaterialApi
@ExperimentalFoundationApi
fun JobResultListView(
  stateFlow: StateFlow<JobResultState>,
  events: JobResultEvents,
) {
  val state by stateFlow.collectAsState()

  val listState = rememberLazyListState()

  val coroutineScope = rememberCoroutineScope()

  val showScrollToTopButton: Boolean by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex > 0
    }
  }

  var jobNameFilter: String by remember { mutableStateOf(state.jobNameFilter) }

  var resultFilter: ResultFilter by remember { mutableStateOf(state.resultFilter) }

  var selectedJob: String by remember { mutableStateOf(state.selectedJob) }

//  LaunchedEffect("jobNameFilter") {
//    snapshotFlow {
//      jobNameFilter
//    }
//      .distinctUntilChanged()
//      .debounce(500)
//      .collect {
//        events.setFilter(
//          jobNamePrefix = it,
//          selectedJob = selectedJob,
//          result = resultFilter
//        )
//      }
//  }

  Column(
    modifier = Modifier.padding(6.dp)
  ) {
    Row {
//      AnimatedVisibility(visible = showScrollToTopButton) {
//        IconButton(onClick = {
//          coroutineScope.launch {
//            listState.animateScrollToItem(index = 0)
//          }
//        }) {
//          Icon(
//            imageVector = Icons.Filled.KeyboardArrowUp,
//            contentDescription = "Scroll to Top",
//          )
//        }
//      }

      Button(
        onClick = {
          events.refresh()
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
        border = BorderStroke(width = 1.dp, color = Color.White),
      ) {
        Text("Refresh", fontWeight = FontWeight.ExtraBold, color = Color.LightGray)
      }

      if (state.latestRefresh != null) {
        Spacer(Modifier.weight(1f))

        Text(
          "Last Refresh: ${abbreviatedTimestampFormat.format(state.latestRefresh)}",
          modifier = Modifier.align(Alignment.CenterVertically),
        )
      }
    }

    Spacer(Modifier.height(6.dp))

    when (val st = state) {
      JobResultState.Initial -> {
        println("${javaClass.simpleName} initialized")
      }
      is JobResultState.Loaded, is JobResultState.Loading -> {
        Row {
          TextField(
            value = st.jobNameFilter,
            onValueChange = {
              jobNameFilter = it

              events.setFilter(
                jobNamePrefix = it,
                selectedJob = selectedJob,
                result = resultFilter
              )
            },
//            onValueChangedFinished = {},
            label = { Text("Job", modifier = Modifier.fillMaxHeight()) },
            maxLines = 1,
            modifier = Modifier.weight(1f).height(65.dp),
          )

          Spacer(Modifier.width(10.dp))

          JobNameDropdown(
            label = "Job Name",
            options = st.jobNameOptions,
            value = selectedJob,
            onValueChange = {
              selectedJob = it

              events.setFilter(
                selectedJob = selectedJob,
                jobNamePrefix = jobNameFilter,
                result = resultFilter,
              )
            }
          )

          Spacer(Modifier.width(10.dp))

          EnumDropdown(
            label = "Status",
            value = resultFilter,
            onValueChange = {
              resultFilter = it

              events.setFilter(
                selectedJob = selectedJob,
                jobNamePrefix = jobNameFilter,
                result = it,
              )
            },
            modifier = Modifier.width(160.dp),
          )
        }

        LazyColumn(
          contentPadding = PaddingValues(horizontal = 2.dp),
          state = listState,
        ) {
          stickyHeader {
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(30.dp).background(color = MaterialTheme.colors.background),
              ) {
                AnimatedVisibility(visible = showScrollToTopButton) {
                  IconButton(onClick = {
                    coroutineScope.launch {
                      listState.animateScrollToItem(index = 0)
                    }
                  }) {
                    Icon(
                      imageVector = Icons.Filled.KeyboardArrowUp,
                      contentDescription = "Scroll to Top",
                    )
                  }
                }

                val startPos = if (showScrollToTopButton) {
                  6.dp
                } else {
                  54.dp
                }

                Text(
                  text = "Job",
                  modifier = Modifier.padding(start = startPos),
                  style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                )

                Spacer(Modifier.weight(1f))

                Text(
                  text = "Execution Time",
                  style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                )

                Spacer(Modifier.width(90.dp))

                Text(
                  text = "Finished",
                  style = TextStyle(
                    fontSize = 16.sp,
//                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold,
                  ),
                )
              }

              Divider(modifier = Modifier.fillMaxWidth(), color = Color.White)
            }
          }

          items(items = st.jobResults) { jobResult ->
            JobResultListViewItem(jobResult = jobResult)
          }
        }
      }
      is JobResultState.Error -> {
        Text(st.errorMessage)
      }
    }

    Spacer(Modifier.weight(1f))

    Text("Status: ${state.status}")
  }
}
