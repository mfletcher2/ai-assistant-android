package lol.max.assistantgpt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class DialogTypes {
    NONE, SETTINGS;
}

val availableModels = listOf("gpt-3.5-turbo", "gpt-3.5-turbo-16k", "gpt-4")

@Composable
fun SettingsDialog(type: DialogTypes, options: Options, onDismissRequest: () -> Unit) {
    if (type == DialogTypes.SETTINGS) {
        var selectedModel by rememberSaveable { mutableStateOf(options.model) }
        AlertDialog(onDismissRequest = { onDismissRequest() },
            confirmButton = {
                TextButton(
                    onClick = {
                        options.model = selectedModel
                        onDismissRequest()
                    },
                    content = { Text(text = "Save and Close") })
            },
            title = { Text(text = "Options") },
            icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Icon") },
            text = {
                Column {
                    Text(text = "Model")
                    Column(Modifier.selectableGroup()) {
                        availableModels.forEach { text ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (text == selectedModel),
                                        onClick = {
                                            selectedModel = text
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (text == selectedModel),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            })
    }
}

data class Options(var model: String)