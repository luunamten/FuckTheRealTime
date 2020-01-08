package xxx.fucktherealtime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainActivity : AppCompatActivity(), SensorEventListener2 {

    private val SERVER_IP: String = "192.168.0.200"
    private val SERVER_PORT: Int = 1234
    private val TAG: String = this.javaClass.name
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSocketHandler: SocketHandler

    // Create a constant to convert nanoseconds to seconds.
    private val EPSILON: Float = 0.0f
    private val NS2S: Float = 1.0f / 1000000000.0f
    private var timestamp: Float = 0f
    private var rotationCurrent: FloatArray = floatArrayOf(
        1.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 1.0f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSocketHandler = SocketHandler(SERVER_IP, SERVER_PORT)
    }

    override fun onStart() {
        super.onStart()
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager.unregisterListener(this)
    }

    override fun onBackPressed() {

    }

    fun AEqualsAxB(A: FloatArray, B: FloatArray) {
        rotationCurrent[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6]
        rotationCurrent[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7]
        rotationCurrent[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8]
        rotationCurrent[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6]
        rotationCurrent[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7]
        rotationCurrent[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8]
        rotationCurrent[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6]
        rotationCurrent[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7]
        rotationCurrent[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8]
    }

    fun matrixToByteBuffer(): ByteBuffer {
        val buffer: ByteBuffer = ByteBuffer.allocate(36)
        buffer.order(mSocketHandler.getTargetByteOrder())
        buffer.putFloat(rotationCurrent[0])
        buffer.putFloat(rotationCurrent[1])
        buffer.putFloat(rotationCurrent[2])
        buffer.putFloat(rotationCurrent[3])
        buffer.putFloat(rotationCurrent[4])
        buffer.putFloat(rotationCurrent[5])
        buffer.putFloat(rotationCurrent[6])
        buffer.putFloat(rotationCurrent[7])
        buffer.putFloat(rotationCurrent[8])
        return buffer
    }

    fun displayMatrix() {
        sensorText.text = "%f %f %f\n%f %f %f\n%f %f %f".format(
            rotationCurrent[0], rotationCurrent[1], rotationCurrent[2],
            rotationCurrent[3], rotationCurrent[4], rotationCurrent[5],
            rotationCurrent[6], rotationCurrent[7], rotationCurrent[8]
        )
    }

    fun getRotationMat3FromVector(R: FloatArray, rotationVector: FloatArray) {
        var q0 = rotationVector[3]
        val q1 = rotationVector[0]
        val q2 = rotationVector[1]
        val q3 = rotationVector[2]
        val sq_q1 = 2f * q1 * q1
        val sq_q2 = 2f * q2 * q2
        val sq_q3 = 2f * q3 * q3
        val q1_q2 = 2f * q1 * q2
        val q3_q0 = 2f * q3 * q0
        val q1_q3 = 2f * q1 * q3
        val q2_q0 = 2f * q2 * q0
        val q2_q3 = 2f * q2 * q3
        val q1_q0 = 2f * q1 * q0
        R[0] = 1f - sq_q2 - sq_q3
        R[1] = q1_q2 - q3_q0
        R[2] = q1_q3 + q2_q0
        R[3] = q1_q2 + q3_q0
        R[4] = 1f - sq_q1 - sq_q3
        R[5] = q2_q3 - q1_q0
        R[6] = q1_q3 - q2_q0
        R[7] = q2_q3 + q1_q0
        R[8] = 1f - sq_q1 - sq_q2
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onFlushCompleted(sensor: Sensor?) {}

    override fun onSensorChanged(event: SensorEvent?) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        val deltaRotationVector: FloatArray = FloatArray(4) { 0f }
        if (timestamp != 0f && event != null) {
            val dT = (event.timestamp - timestamp) * NS2S
            // Axis of the rotation sample, not normalized yet.
            var axisX: Float = event.values[0]
            var axisY: Float = event.values[1]
            var axisZ: Float = event.values[2]

            // Calculate the angular speed of the sample
            val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            } else {
                timestamp = event.timestamp.toFloat()
                return
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo: Float = sin(thetaOverTwo)
            val cosThetaOverTwo: Float = cos(thetaOverTwo)
            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo
        }
        timestamp = event?.timestamp?.toFloat() ?: 0f
        val deltaRotationMatrix = FloatArray(9) { 0f }
        getRotationMat3FromVector(deltaRotationMatrix, deltaRotationVector)
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        val copyRotationCurrent: FloatArray = rotationCurrent.copyOf()
        AEqualsAxB(copyRotationCurrent, deltaRotationMatrix)
        val buffer: ByteBuffer = matrixToByteBuffer()
        mSocketHandler.sendBytes(buffer.array())
        //Display matrix
        displayMatrix()
    }

    fun resetRotation(view: View) {
        rotationCurrent = floatArrayOf(
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
        )
    }
}
