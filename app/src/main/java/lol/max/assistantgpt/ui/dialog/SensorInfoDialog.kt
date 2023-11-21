package lol.max.assistantgpt.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import lol.max.assistantgpt.R
import lol.max.assistantgpt.api.SensorValues

@Composable
fun SensorInfoDialog(sensorValues: MutableState<SensorValues>, onDismissRequest: () -> Unit) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(text = stringResource(id = R.string.close))

        }
    }, title = { Text(text = stringResource(R.string.sensor_values_title)) }, icon = {
        Icon(
            painter = painterResource(id = R.drawable.baseline_sensors_24),
            contentDescription = "sensors"
        )
    }, text = {
        Text(text = "Accelerometer:\n${sensorValues.value.accelerometer}")
    })
}