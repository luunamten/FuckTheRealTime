package xxx.fucktherealtime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SensorEventListener2 {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private var mTimeStamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    }

    override fun onStart() {
        super.onStart()
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onFlushCompleted(sensor: Sensor?) {}

    override fun onSensorChanged(event: SensorEvent?) {
        sensorText.text = "X: %f\nY: %f\nZ: %f".format(event!!.values[0], event!!.values[1], event!!.values[2])
        mTimeStamp = event!!.timestamp
    }
}
