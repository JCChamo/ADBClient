package com.example.adb

import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import com.example.adb.interfaces.DeviceConnectionListener
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket

class DeviceConnection(var host : String, var port : Int, var listener: DeviceConnectionListener) : Closeable {

    private val CONN_TIMEOUT = 5000


    private var connection: AdbConnection? = null
    private var shellStream: AdbStream? = null
    private var closed = false

    private fun startConnect(){
        Thread(Runnable {

            var connected = false
            var socket = Socket()

            var crypto : AdbCrypto = listener.loadAdbCrypto(this@DeviceConnection)!!

            try {
                socket.connect(InetSocketAddress(host, port), CONN_TIMEOUT)
            } catch (e : Exception) {
                listener.notifyConnectionFailed(this@DeviceConnection, e)
            }

            try {
                connection = AdbConnection.create(socket, crypto)
                connection?.connect()
            } catch (e : Exception) {
                listener.notifyConnectionFailed(this@DeviceConnection, e)
            } finally {
                if (!connected){
                    MainActivity.safeClose(shellStream)
                    if (!MainActivity.safeClose(connection)){
                        try {
                            socket.close()
                        } catch (e : Exception) {

                        }
                    return@Runnable
                    }
                }
            }


            listener.notifyConnectionEstablished(this@DeviceConnection)
            startReceiveThread()

        }).start()

    }

    private fun startReceiveThread() {
        Thread(Runnable {
            try {
                while (!shellStream?.isClosed!!){
                    var data = shellStream!!.read()
                    listener.receivedData(this@DeviceConnection, data, 0, data.size)
                }
                listener.notifyStreamClosed(this@DeviceConnection)
            } catch (e : Exception) {
                listener.notifyStreamFailed(this@DeviceConnection, e)
            } catch (e : Exception) {
            } finally {
                MainActivity.safeClose(this@DeviceConnection)
            }
        }).start()
    }

    private fun isClosed() : Boolean = closed

    override fun close() {
        if (isClosed())
            return
        else
            closed = true

        MainActivity.safeClose(shellStream)
        MainActivity.safeClose(connection)
    }

}