package xxx.fucktherealtime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.net.SocketException

class MainActivity : AppCompatActivity(), SensorEventListener2 {

    private val TAG: String = this.javaClass.name
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSocketHandler: SocketHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSocketHandler = SocketHandler("192.168.0.200", 1234)
    }

    override fun onStart() {
        super.onStart()
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onFlushCompleted(sensor: Sensor?) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val strData: String = "X: %f | Y: %f | Z: %f\n\u0000".format(
            event!!.values[0],
            event!!.values[1],
            event!!.values[2]
        )
        sensorText.text = strData
        mSocketHandler.sendString(strData)
    }
}
