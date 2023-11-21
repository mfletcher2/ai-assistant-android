package lol.max.assistantgpt.ui.dialog

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.availableModels
import lol.max.assistantgpt.ui.viewmodel.Options

enum class DialogTypes {
    NONE, SETTINGS, INFO, VOICE, SENSOR, SENSOR_INFO;
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(options: Options, onDismissRequest: () -> Unit, onSaveRequest: () -> Unit = {}) {
    var selectedModel by rememberSaveable { mutableStateOf(options.model.name) }
    var timeoutSec by rememberSaveable { mutableStateOf(options.timeoutSec.toString()) }
    var allowSensors by rememberSaveable { mutableStateOf(options.allowSensors) }
    var confirmSensors by rememberSaveable { mutableStateOf(options.confirmSensors) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    options.model =
                        availableModels.find { it.name == selectedModel } ?: availableModels[0]
                    try {
                        val intTimeoutSec = Integer.parseInt(timeoutSec)
                        if (intTimeoutSec > 0) options.timeoutSec = intTimeoutSec
                    } catch (_: NumberFormatException) {
                    }
                    options.allowSensors = allowSensors
                    options.confirmSensors = confirmSensors
                    onSaveRequest()
                    onDismissRequest()
                },
                content = { Text(text = stringResource(R.string.save_and_close)) })
        },
        title = { Text(text = stringResource(R.string.options)) },
        icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Icon") },
        text = {
            LazyColumn {
                item { Text(text = stringResource(R.string.model)) }
                items(count = availableModels.size) { i ->
                    Column(Modifier.selectableGroup()) {
                        OptionsItem(
                            selected = availableModels[i].name == selectedModel,
                            enabled = true,
                            role = Role.RadioButton,
                            onClick = { selectedModel = availableModels[i].name })
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (availableModels[i].name == selectedModel),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = availableModels[i].name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(8.dp, 0.dp)
                                )
                            }
                        }

                    }
                }
                item {
                    Text(text = stringResource(R.string.api))
                }
                item {
                    TextField(
                        value = timeoutSec,
                        onValueChange = { if (it.isDigitsOnly()) timeoutSec = it },
                        label = { Text(text = stringResource(R.string.timeout_seconds)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(16.dp, 8.dp)
                    )
                }
                item {
                    Text(text = stringResource(R.string.privacy))
                }
                item {
                    OptionsItem(
                        selected = allowSensors,
                        enabled = true,
                        role = Role.Switch,
                        onClick = { allowSensors = !allowSensors }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.allow_sensors),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                            )
                            Switch(
                                checked = allowSensors,
                                onCheckedChange = null,
                            )
                        }

                    }
                }
                item {
                    OptionsItem(
                        selected = confirmSensors,
                        enabled = allowSensors,
                        role = Role.Switch,
                        onClick = { confirmSensors = !confirmSensors }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Confirm sensors",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (allowSensors) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.5f
                                ),
                            )
                            Switch(
                                checked = confirmSensors,
                                onCheckedChange = null,
                                enabled = allowSensors
                            )
                        }
                    }
                }
            }

        })
}

@Composable
fun OptionsItem(
    selected: Boolean,
    enabled: Boolean,
    role: Role?,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = role
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
