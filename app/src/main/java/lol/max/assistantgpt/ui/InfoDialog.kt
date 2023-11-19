package lol.max.assistantgpt.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import lol.max.assistantgpt.R

@Composable
fun InfoDialog(onDismissRequest: () -> Unit = {}) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(text = stringResource(R.string.close))

        }
    }, icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Info") },
        title = { Text(text = stringResource(R.string.about)) }, text = {
            Text(
                text = stringResource(R.string.info_dialog),
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            )
        }, properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}