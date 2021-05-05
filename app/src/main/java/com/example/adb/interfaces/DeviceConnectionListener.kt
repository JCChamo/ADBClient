package com.example.adb.interfaces

import com.cgutman.adblib.AdbCrypto
import com.example.adb.DeviceConnection

interface DeviceConnectionListener {

    fun notifyConnectionEstablished(devConn: DeviceConnection?)

    fun notifyConnectionFailed(
        devConn: DeviceConnection?,
        e: Exception?
    )

    fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?)

    fun notifyStreamClosed(devConn: DeviceConnection?)

    fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto?

    fun canReceiveData(): Boolean

    fun receivedData(
        devConn: DeviceConnection?,
        data: ByteArray?,
        offset: Int,
        length: Int
    )
}