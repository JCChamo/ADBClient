package com.example.adb


import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.cgutman.adblib.AdbCrypto
import com.example.adb.interfaces.DeviceConnectionListener

class ShellService : Service(), DeviceConnectionListener {

    private val binder = ShellServiceBinder()
    private val listener = ShellListener(this)

    private val currentConnectionMap = hashMapOf<String, DeviceConnection>()


    inner class ShellServiceBinder : Binder(){
        fun createConnection(host : String, port : Int): DeviceConnection {
            val conn = DeviceConnection(listener, host, port)
            listener.addListener(conn, this@ShellService)
            Log.d(":::", "Conexión creada")
            return conn
        }

        fun findConnection(host : String, port : Int): DeviceConnection? {
            val connString = "$host:$port"
            Log.d(":::", "Conexión encontrada")
            return currentConnectionMap[connString]
        }

        fun addListener(
            conn: DeviceConnection?,
            listener: DeviceConnectionListener?
        ) {
            this@ShellService.listener.addListener(conn!!, listener!!)
        }

        fun removeListener(
            conn: DeviceConnection?,
            listener: DeviceConnectionListener?
        ) {
            this@ShellService.listener.removeListener(conn, listener!!)
        }
    }

    private fun addNewConnection(devConn: DeviceConnection) {
        currentConnectionMap[getConnectionString(devConn)!!] = devConn
    }

    private fun removeConnection(devConn: DeviceConnection) {
        currentConnectionMap.remove(getConnectionString(devConn))

        if (currentConnectionMap.isEmpty()) {
            stopSelf()
        }
    }

    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {
        if (devConn != null) {
            addNewConnection(devConn)
        }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
    }

    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        removeConnection(devConn!!)
    }

    override fun notifyStreamClosed(devConn: DeviceConnection?) {
        removeConnection(devConn!!)
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? = null

    override fun canReceiveData(): Boolean = false

    override fun receivedData(
        devConn: DeviceConnection?,
        data: ByteArray?,
        offset: Int,
        length: Int
    ) {
    }

    override fun onBind(intent: Intent?): IBinder? = binder
    private fun getConnectionString(devConn: DeviceConnection): String? {
        return devConn.getmHost() + ":" + devConn.getmPort()
    }

}