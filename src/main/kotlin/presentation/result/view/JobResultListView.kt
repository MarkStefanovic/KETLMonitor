package presentation.result.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import presentation.result.bloc.JobResultEvents
import presentation.result.bloc.JobResultState
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

  Column(
    modifier = Modifier.padding(6.dp)
  ) {
    Row {
      Button(
        onClick = events::refresh,
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
        Column {
          TextField(
            value = st.jobNameFilter,
            onValueChange = { txt ->
              events.setJobNameFilter(txt)
            },
            label = { Text("Job", modifier = Modifier.fillMaxHeight()) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
          )

          LazyColumn(
            contentPadding = PaddingValues(horizontal = 2.dp),
            state = listState,
          ) {
            items(items = st.jobResults) { jobResult ->
              JobResultListViewItem(jobResult = jobResult)
            }
          }
        }
        Spacer(Modifier.weight(1f))

        Text("Status: ${state.status}")
      }
      is JobResultState.Error -> {
        Text(st.errorMessage)
      }
    }
  }
}
