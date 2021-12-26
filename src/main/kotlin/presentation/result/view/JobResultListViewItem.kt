package presentation.result.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import domain.JobResult
import presentation.shared.abbreviatedTimestampFormat

@Composable
@ExperimentalMaterialApi
fun JobResultListViewItem(
  jobResult: JobResult,
) {
  var expanded: Boolean by remember { mutableStateOf(false) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
  ) {
    if (jobResult.errorMessage != null) {
      if (expanded) {
        IconButton(
          onClick = { expanded = !expanded },
          modifier = Modifier.align(Alignment.Top),
        ) {
          Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Collapse Error",
          )
        }

        Column {
          Text(
            text = jobResult.jobName,
            style = MaterialTheme.typography.body1,
            color = Color.Red,
          )

          Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 1.dp,
          ) {
            Text(
              text = jobResult.errorMessage,
              modifier = Modifier.padding(all = 4.dp),
              maxLines = if (expanded) Int.MAX_VALUE else 1,
              style = MaterialTheme.typography.body2,
            )
          }
        }
      } else {
        IconButton(
          onClick = { expanded = !expanded }
        ) {
          Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = "Expand Error",
          )
        }

        Text(
          text = jobResult.jobName,
          style = MaterialTheme.typography.body1,
          color = Color.Red,
        )
      }
    } else {
      val imageVector = if (jobResult.skipReason != null) {
        Icons.Filled.Refresh
      } else {
        Icons.Filled.Done
      }

      IconButton(
        onClick = { expanded = !expanded }
      ) {
        Icon(
          imageVector = imageVector,
          contentDescription = "Expand Error",
        )
      }

      Text(
        text = jobResult.jobName,
        modifier = Modifier.padding(all = 4.dp),
        style = MaterialTheme.typography.body1,
      )
    }

    Spacer(Modifier.weight(1f))

    Text(text = jobResult.end.format(abbreviatedTimestampFormat))
  }
}
