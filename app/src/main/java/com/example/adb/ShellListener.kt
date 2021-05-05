package com.example.adb

import com.cgutman.adblib.AdbCrypto
import com.example.adb.interfaces.DeviceConnectionListener
import java.util.*

class ShellListener(var service: ShellService) : DeviceConnectionListener{

    private var listenerMap = hashMapOf<DeviceConnection, LinkedList<DeviceConnectionListener>>()

    fun addListener(conn : DeviceConnection, listener: DeviceConnectionListener) {
        var listeners = listenerMap[conn]
        if (listeners != null)
            listeners.add(listener)
        else {
            listeners = LinkedList()
            listeners.add(listener)
            listenerMap[conn] = listeners
        }
    }

    @Synchronized
    fun removeListener(
        conn: DeviceConnection?,
        listener: DeviceConnectionListener
    ) {
        val listeners = listenerMap[conn]
        listeners?.remove(listener)
    }

    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {

        val listeners = listenerMap[devConn]
        if (listeners != null) {
            for (listener in listeners) {
                listener.notifyConnectionEstablished(devConn)
            }
        }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
        val listeners = listenerMap[devConn]
        if (listeners != null) {
            for (listener in listeners) {
                listener.notifyConnectionFailed(devConn, e)
            }
        }
    }

    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        val listeners = listenerMap[devConn]
        if (listeners != null) {
            for (listener in listeners) {
                listener.notifyStreamFailed(devConn, e)
            }
        }
    }

    override fun notifyStreamClosed(devConn: DeviceConnection?) {
        val listeners = listenerMap[devConn]
        if (listeners != null) {
            for (listener in listeners) {
                listener.notifyStreamClosed(devConn)
            }
        }
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? = MainActivity.crypto

    override fun canReceiveData(): Boolean = true

    override fun receivedData(
        devConn: DeviceConnection?,
        data: ByteArray?,
        offset: Int,
        length: Int
    ) {
    }

}