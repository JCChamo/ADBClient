package com.example.adb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Base64.encodeToString
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.File
import java.io.IOException
import java.net.Socket


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var defaultValuesButton: Button
    private lateinit var connectButton: Button
    private lateinit var rebootButton: Button
    private lateinit var killButton: Button
    private lateinit var listDevicesButton: Button
    private lateinit var mWifi : NetworkInfo
    private lateinit var connManager : ConnectivityManager
    private lateinit var ip : EditText
    private lateinit var port : EditText
    private lateinit var commandListView : ListView
    private lateinit var commandList : ArrayList<String>
    private lateinit var adapter : ArrayAdapter<String>
    private lateinit var mProgressBar: ProgressBar
    private lateinit var  pub : File
    private lateinit var priv : File
    private lateinit var stream: AdbStream
    private lateinit var connection: AdbConnection

    private lateinit var socket: Socket
    private lateinit var crypto: AdbCrypto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultValuesButton = findViewById(R.id.defaultValuesButton)
        connectButton = findViewById(R.id.connectButton)
        rebootButton = findViewById(R.id.rebootButton)
        killButton = findViewById(R.id.killButton)
        listDevicesButton = findViewById(R.id.listDevicesButton)
        ip = findViewById(R.id.server)
        port = findViewById(R.id.port)
        commandListView = findViewById(R.id.commandListView)
        mProgressBar = findViewById(R.id.progressbar)


        commandListView.setBackgroundColor(Color.BLACK)

        connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!

        pub = File(filesDir, "adbkey.pub")
        priv = File(filesDir, "adbkey")

        defaultValuesButton.setOnClickListener(this)
        connectButton.setOnClickListener(this)
        rebootButton.setOnClickListener(this)
        killButton.setOnClickListener(this)
        listDevicesButton.setOnClickListener(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10
            )
        }

        mProgressBar.visibility = View.GONE

        commandList = arrayListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, commandList)
        commandListView.adapter = adapter


    }

    override fun onResume() {
        super.onResume()
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.defaultValuesButton -> {
                ip.setText("172.20.255.31")
                port.setText("5555")
                connectButton.visibility = View.VISIBLE
            }
            R.id.connectButton -> {
                if (mWifi.isConnected) {
                    Toast.makeText(applicationContext, "CONECTADO", Toast.LENGTH_SHORT).show()
                    progressBarAction()
                    defaultValuesButton.visibility = View.INVISIBLE
                    addCommandToList("adb tcpip ${port.text}")
                    addCommandToList("adb connect ${ip.text}")
                    rebootButton.visibility = View.VISIBLE
                    killButton.visibility = View.VISIBLE
                    listDevicesButton.visibility = View.VISIBLE
                    startConnection()
                } else
                    Toast.makeText(applicationContext, "CONÉCTESE AL WIFI", Toast.LENGTH_SHORT)
                        .show()
            }
            R.id.rebootButton -> {
//                addCommandToList("adb push patata.txt /sdcard/patata.txt")
                addCommandToList("adb reboot")
                sendCommand("reboot")
            }

            R.id.killButton -> {
                addCommandToList("adb kill-server")
                disconnect()
            }

            R.id.listDevicesButton -> {
                addCommandToList("adb devices")
//                sendCommand("input keyevent POWER")
                sendCommand("devices")
//                defaultValuesButton.visibility = View.VISIBLE
            }
        }
    }

    private fun progressBarAction(){
        mProgressBar.visibility = View.VISIBLE
        Handler().postDelayed({
            mProgressBar.visibility = View.GONE
        }, 2000)
    }

    private fun addCommandToList(command : String) {
        commandList.add(command)
        adapter.notifyDataSetChanged()
        commandListView.post { commandListView.setSelection(commandListView.count - 1) }
    }

    private fun clearCommandList() {
        commandList.clear()
        adapter.notifyDataSetChanged()
    }

    private fun sendCommand(command: String) {
        Thread(Runnable {
            openStream()
            try {
                stream.write(command + '\n')
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.d(":::", "Comando enviado")
            getResponse()
        }).start()
    }

    private fun openStream() {
        try {
            stream = connection.open(":shell")
        } catch (e : java.lang.Exception) {
            e.printStackTrace()
        }
        Log.d(":::", "Flujo abierto")
    }

    private fun getResponse(){
        try {
            val responseBytes = stream.read()
            runOnUiThread{
                addCommandToList(String(responseBytes, charset("UTF-8")))
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        Log.d(":::", "Respuesta recibida")
//        stream.close()
//        Log.d(":::", "Flujo cerrado")
    }

    private fun disconnect(){
        try {
            connection.close()
        } catch (e : Exception) {
            e.printStackTrace()
        }
        Log.d(":::", "Conexión cerrada")
    }

    private fun startConnection(){
        Thread(Runnable {
            try {
                crypto = setupCrypto()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                socket = Socket(ip.text.toString(), Integer.parseInt(port.text.toString()))
            } catch (e : Exception) {
                e.printStackTrace()
            }
            Log.d(":::", "Socket conectado")
            connection = AdbConnection.create(socket, crypto)
            Log.d(":::", "Conexión creada")

            try {
                connection.connect()
            } catch (e : Exception) {
                e.printStackTrace()
            }

            Log.d(":::", "ADB conectado")
        }).start()
    }

    private fun setupCrypto(): AdbCrypto {
        var adbCrypto : AdbCrypto? = null
        if (pub.exists() && priv.exists()) {
            Log.d(":::", "Las claves existen")
            adbCrypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), priv, pub)
            } catch (e : Exception) {
                null
            }
            Log.d(":::", "setupCrypto: Crypto creado")
        }
        if (adbCrypto == null) {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(priv, pub)
        }
        return adbCrypto!!
    }

    class AndroidBase64 : AdbBase64 {
        override fun encodeToString(data: ByteArray): String {
            return encodeToString(data, Base64.NO_WRAP)
        }
    }

}