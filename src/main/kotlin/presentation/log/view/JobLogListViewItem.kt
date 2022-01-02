package presentation.log.view

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.JobLogEntry
import domain.LogLevel
import presentation.shared.abbreviatedTimestampFormat

@Composable
@ExperimentalMaterialApi
fun JobLogListViewItem(
  logEntry: JobLogEntry,
) {
  var expanded: Boolean by remember { mutableStateOf(false) }

  val icon = if (expanded) {
    Icons.Filled.KeyboardArrowUp
  } else {
    Icons.Filled.KeyboardArrowRight
  }

  val jobNameColor = when (logEntry.logLevel) {
    LogLevel.Debug -> Color.LightGray
    LogLevel.Error -> Color.Red
    LogLevel.Info -> Color.White
    LogLevel.Warning -> Color.Yellow
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
  ) {

    IconButton(
      onClick = { expanded = !expanded },
      modifier = Modifier.align(Alignment.Top),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = "Collapse",
      )
    }

    Text(
      text = logEntry.jobName,
      style = MaterialTheme.typography.body1,
      color = jobNameColor,
    )

    Spacer(Modifier.weight(1f))

    Text(text = logEntry.ts.format(abbreviatedTimestampFormat))
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    elevation = 1.dp,
  ) {
    Text(
      text = logEntry.message,
      modifier = Modifier.padding(all = 4.dp),
      maxLines = if (expanded) Int.MAX_VALUE else 1,
      softWrap = true,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.body2,
    )
  }
}
