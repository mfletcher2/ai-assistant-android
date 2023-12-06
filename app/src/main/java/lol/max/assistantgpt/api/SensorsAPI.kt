package lol.max.assistantgpt.api

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SensorFunctions(context: Context) : SensorEventListener {
    private var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var light: Sensor? = null
    private var magneticField: Sensor? = null
    private var pressure: Sensor? = null
    private var stepCounter: Sensor? = null
    val sensorValues = SensorValues()

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorValues.sensorList =
            sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.stringType }
    }

    fun registerListeners() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregisterListeners() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> sensorValues.accelerometer = event.values.toList()
            Sensor.TYPE_LIGHT -> sensorValues.light = event.values[0]
            Sensor.TYPE_MAGNETIC_FIELD -> sensorValues.magneticField = event.values.toList()
            Sensor.TYPE_PRESSURE -> sensorValues.pressure = event.values[0]
            Sensor.TYPE_STEP_COUNTER -> sensorValues.stepCounter = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    class OrientationRequest : LateResponse() {
        fun getOrientation(sensorValues: SensorValues) {
            val rotation = FloatArray(9)
            SensorManager.getRotationMatrix(
                rotation,
                null,
                sensorValues.accelerometer.toFloatArray(),
                sensorValues.magneticField.toFloatArray()
            )

            val result = FloatArray(3)
            SensorManager.getOrientation(rotation, result)
            onSuccess(mapOf("azimuth" to result[0], "pitch" to result[1], "roll" to result[2]))
        }
    }
}

class SensorValues {
    var accelerometer by mutableStateOf(listOf<Float>())
    var light by mutableStateOf(0f)
    var magneticField by mutableStateOf(listOf<Float>())
    var pressure by mutableStateOf(0f)
    var stepCounter by mutableStateOf(0)
    var sensorList: List<String> = listOf()
}