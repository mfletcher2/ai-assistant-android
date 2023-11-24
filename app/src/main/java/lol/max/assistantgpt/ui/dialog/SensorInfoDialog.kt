package lol.max.assistantgpt.ui.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lol.max.assistantgpt.api.SensorValues

@Composable
fun SensorInfoDialog(sensorValues: SensorValues, onDismissRequest: () -> Unit) {
    val text = "Accelerometer:\n${sensorValues.accelerometer} m/s2\n\n" +
            "Light: ${sensorValues.light} lux\n\n" +
            "Magnetic field:\n${sensorValues.magneticField} μT\n\n" +
            "Pressure: ${sensorValues.pressure} hPa\n\n" +
            "Step counter: ${sensorValues.stepCounter} steps\n\n" +
            "Sensor list:\n${sensorValues.sensorList}"

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Text(
            text = text,
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp), color = Color.White
        )
    }
}