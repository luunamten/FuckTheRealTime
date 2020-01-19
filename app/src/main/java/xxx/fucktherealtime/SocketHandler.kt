package xxx.fucktherealtime

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.OutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SocketHandler {

    private val FLOAT_BYTES: Int = 4
    private var mConnectTimeout = 1000
    private var mInetAddress: InetSocketAddress
    private var mSocketThread: HandlerThread
    private var mSocketThreadHandler: Handler
    private var mTargetByteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    private lateinit var mSocket: Socket

    constructor(serverIP: String, serverPort: Int) {
        mInetAddress = InetSocketAddress(serverIP, serverPort)
        mSocketThread = HandlerThread("Socket")
        mSocketThread.start()
        mSocketThreadHandler = Handler(mSocketThread.looper)
        mSocketThreadHandler.post {
            connect()
        }
    }

    private fun connect() {
        mSocket = Socket()
        try {
            mSocket.connect(mInetAddress, mConnectTimeout)
        } catch (exp: Exception) {
            exp.printStackTrace()
        }
    }

    fun setAddress(serverIP: String, serverPort: Int) {
        mInetAddress = InetSocketAddress(serverIP,serverPort)
        try {
            mSocket.close()
            mSocketThreadHandler.post {
                connect()
            }
        } catch (exp: Exception) {
            exp.printStackTrace()
        }
    }

    fun sendString(data: String) {
        mSocketThreadHandler.post {
            try {
                val stream: OutputStream = mSocket.getOutputStream()
                stream.write(data.toByteArray())
            } catch (socketExp: SocketException) {
                socketExp.printStackTrace()
                connect()
            }
        }
    }

    fun sendBytes(data: ByteArray) {
        mSocketThreadHandler.post {
            try {
            val stream: OutputStream = mSocket.getOutputStream()
            stream.write(data)
            } catch (socketExp: SocketException) {
                socketExp.printStackTrace()
                connect()
            }
        }
    }

    fun sendBytes(buffer: ByteBuffer, useTargetByteOrder: Boolean) {
        mSocketThreadHandler.post {
            try {
                val stream: OutputStream = mSocket.getOutputStream()
                if(useTargetByteOrder) {
                    buffer.order(mTargetByteOrder)
                }
                stream.write(buffer.array())
            } catch (socketExp: SocketException) {
                socketExp.printStackTrace()
                connect()
            }
        }
    }

    fun sendFloat(data: Float) {
        mSocketThreadHandler.post {
            try {
                val stream: OutputStream = mSocket.getOutputStream()
                val buffer: ByteBuffer = ByteBuffer.allocate(FLOAT_BYTES)
                buffer.order(mTargetByteOrder)
                buffer.putFloat(data)
                stream.write(buffer.array())
            } catch (socketExp: SocketException) {
                socketExp.printStackTrace()
                connect()
            }
        }
    }

    fun setTargetByteOrder(byteOrder: ByteOrder) {
        mTargetByteOrder = byteOrder
    }

    fun getTargetByteOrder(): ByteOrder {
        return mTargetByteOrder
    }

    fun setConnectTimeout(timeOut: Int) {
        mConnectTimeout = timeOut
    }

    fun getConnectTimeout(): Int {
        return mConnectTimeout
    }

    fun close() {
        mSocketThread.quitSafely();
        mSocket.close()
    }
}