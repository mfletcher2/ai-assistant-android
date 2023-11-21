package lol.max.assistantgpt.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import lol.max.assistantgpt.R

data class SensorRequest(
    var sensorName: String,
    var permission: String,
    var onGranted: () -> Unit,
    var onDenied: () -> Unit,
    val onClose: () -> Unit,
    var showDialog: () -> Unit
)

@Composable
fun SensorRequestDialog(sensorRequest: SensorRequest, skip: Boolean = false) {
    if (!skip)
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = { sensorRequest.onGranted(); sensorRequest.onClose() }) {
                    Text(text = stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { sensorRequest.onDenied(); sensorRequest.onClose() }) {
                    Text(text = stringResource(R.string.deny))
                }
            },
            title = {
                Text(
                    text = stringResource(
                        R.string.sensor_request,
                        sensorRequest.sensorName
                    )
                )
            })
    else {
        sensorRequest.onClose(); sensorRequest.onGranted()
    }
}