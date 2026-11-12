// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * The red delete item of an [EditDeleteMenu].
 *
 * @property label text of the item.
 * @property testTag test tag of the item.
 * @property onClick called when the item is picked.
 */
class DeleteMenuItem(val label: String, val testTag: String, val onClick: () -> Unit)

/**
 * A "more actions" button that opens a Material 3 dropdown with an edit item and, when [delete] is
 * given, a red delete item. Picking an item closes the menu, then calls its callback.
 *
 * @param moreActionsDescription accessibility label of the "more actions" button.
 * @param moreActionsTag test tag of the "more actions" button.
 * @param editLabel text of the edit item.
 * @param editTag test tag of the edit item.
 * @param onEdit called when the edit item is picked.
 * @param delete the delete item, or null for a menu that cannot delete.
 */
@Composable
fun EditDeleteMenu(
    moreActionsDescription: String,
    moreActionsTag: String,
    editLabel: String,
    editTag: String,
    onEdit: () -> Unit,
    delete: DeleteMenuItem? = null,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(moreActionsTag)) {
      Icon(Icons.Filled.MoreVert, contentDescription = moreActionsDescription)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
          text = { Text(editLabel) },
          onClick = {
            expanded = false
            onEdit()
          },
          leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
          modifier = Modifier.testTag(editTag),
      )
      if (delete != null) {
        DropdownMenuItem(
            text = { Text(delete.label, color = MaterialTheme.colorScheme.error) },
            onClick = {
              expanded = false
              delete.onClick()
            },
            leadingIcon = {
              Icon(
                  Icons.Filled.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
              )
            },
            colors =
                MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            modifier = Modifier.testTag(delete.testTag),
        )
      }
    }
  }
}
