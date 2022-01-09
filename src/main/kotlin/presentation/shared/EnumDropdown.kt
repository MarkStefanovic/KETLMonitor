package presentation.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
inline fun <reified T : Enum<T>> EnumDropdown(
  label: String,
  value: T,
  crossinline onValueChange: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Box(
    modifier = modifier.wrapContentSize(Alignment.TopStart),
  ) {
    OutlinedTextField(
      value = value.toString(),
      onValueChange = {
        onValueChange(value)
      },
      label = { Text(label) },
      trailingIcon = {
        Icon(
          Icons.Filled.ArrowDropDown,
          "contentDescription",
          Modifier.clickable { expanded = !expanded },
        )
      },
    )

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      iterator<T>().asSequence().forEach { enumValue ->
        DropdownMenuItem(onClick = {
          onValueChange(enumValue)
          expanded = false
        }) {
          Text(text = enumValue.toString(), modifier = Modifier.height(30.dp))
        }
      }
    }
  }
}

inline fun <reified T : Enum<T>> iterator(): Iterator<T> = enumValues<T>().iterator()
