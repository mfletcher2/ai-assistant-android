package lol.max.assistantgpt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import lol.max.assistantgpt.ui.viewmodel.Options

enum class DialogTypes {
    NONE, SETTINGS;
}

val availableModels = listOf("gpt-3.5-turbo", "gpt-3.5-turbo-16k", "gpt-4")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(options: Options, onDismissRequest: () -> Unit, onSaveRequest: () -> Unit = {}) {
    var selectedModel by rememberSaveable { mutableStateOf(options.model) }
    var timeoutSec by rememberSaveable { mutableStateOf(options.timeoutSec.toString()) }
    AlertDialog(onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    options.model = selectedModel
                    try {
                        val intTimeoutSec = Integer.parseInt(timeoutSec)
                        if (intTimeoutSec > 0) options.timeoutSec = intTimeoutSec
                    } catch (_: NumberFormatException) {
                    }
                    onSaveRequest()
                    onDismissRequest()
                },
                content = { Text(text = "Save and Close") })
        },
        title = { Text(text = "Options") },
        icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Icon") },
        text = {
            LazyColumn {
                item { Text(text = "Model") }
                items(count = availableModels.size) {
                    Column(Modifier.selectableGroup()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (availableModels[it] == selectedModel),
                                    onClick = {
                                        selectedModel = availableModels[it]
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (availableModels[it] == selectedModel),
                                onClick = null // null recommended for accessibility with screenreaders
                            )
                            Text(
                                text = availableModels[it],
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                    }
                }
                item {
                    Text(text = "API")
                }
                item {
                    TextField(
                        value = timeoutSec,
                        onValueChange = { if (it.isDigitsOnly()) timeoutSec = it },
                        label = { Text(text = "Timeout seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }

        })
}