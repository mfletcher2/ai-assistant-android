package lol.max.assistantgpt.api

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.theokanning.openai.completion.chat.ChatFunction

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

    val accelerometerChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_accelerometer")
        .description("Get the current device's accelerometer x, y, and z values in meters per second squared. The accelerometer takes into account acceleration due to gravity.")
        .executor(Functions.LateResponse::class.java) { it.onSuccess(sensorValues.accelerometer) }
        .build()
    val lightChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_light")
        .description("Get the current device's light sensor value in lux.")
        .executor(Functions.LateResponse::class.java) { it.onSuccess(sensorValues.light) }
        .build()
    val orientationChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_orientation")
        .description("Get the current device's orientation azimuth, pitch, and roll values in radians.")
        .executor(OrientationRequest::class.java) {
            it.getOrientation(
                sensorValues.accelerometer.toFloatArray(),
                sensorValues.magneticField.toFloatArray()
            )
        }
        .build()
    val pressureChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_pressure")
        .description("Get the current device's pressure sensor value in hectopascals.")
        .executor(Functions.LateResponse::class.java) { it.onSuccess(sensorValues.pressure) }
        .build()
    val stepCounterChatFunction: ChatFunction = ChatFunction.builder()
        .name("get_step_counter")
        .description("Get the current device's step counter value. This value counts the number of steps taken since the device was last rebooted.")
        .executor(Functions.LateResponse::class.java) { it.onSuccess(sensorValues.stepCounter) }
        .build()

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

    class OrientationRequest : Functions.LateResponse() {
        fun getOrientation(accelerometer: FloatArray, magneticField: FloatArray) {
            val rotation = FloatArray(9)
            SensorManager.getRotationMatrix(rotation, null, accelerometer, magneticField)

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