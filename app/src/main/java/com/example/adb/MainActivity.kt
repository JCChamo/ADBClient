package com.example.adb

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cgutman.adblib.AdbCrypto
import com.example.adb.interfaces.AndroidBase64
import java.io.Closeable
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var defaultValuesButton: Button
    private lateinit var connectButton: Button
    private lateinit var sendFileButton: Button
    private lateinit var killButton: Button
    private lateinit var listDevicesButton: Button
    private lateinit var mWifi : NetworkInfo
    private lateinit var connManager : ConnectivityManager
    private lateinit var server : EditText
    private lateinit var port : EditText
    private lateinit var commandListView : ListView
    private lateinit var commandList : ArrayList<String>
    private lateinit var adapter : ArrayAdapter<String>
    private lateinit var mProgressBar: ProgressBar
    private var crypto: AdbCrypto? = null
    //private var binder : ShellServiceBinder

    val PUBLIC_KEY_NAME = "public.key"
    val PRIVATE_KEY_NAME = "private.key"

    companion object {
        fun safeClose(c: Closeable?): Boolean {
            if (c == null) return false
            try {
                c.close()
            } catch (e: IOException) {
                return false
            }
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultValuesButton = findViewById(R.id.defaultValuesButton)
        connectButton = findViewById(R.id.connectButton)
        sendFileButton = findViewById(R.id.sendFileButton)
        killButton = findViewById(R.id.killButton)
        listDevicesButton = findViewById(R.id.listDevicesButton)
        server = findViewById(R.id.server)
        port = findViewById(R.id.port)
        commandListView = findViewById(R.id.commandListView)
        mProgressBar = findViewById(R.id.progressbar)


        commandListView.setBackgroundColor(Color.BLACK)

        connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!

        defaultValuesButton.setOnClickListener(this)
        connectButton.setOnClickListener(this)
        sendFileButton.setOnClickListener(this)
        killButton.setOnClickListener(this)
        listDevicesButton.setOnClickListener(this)

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
        when(v?.id) {
            R.id.defaultValuesButton -> {
                server.setText("172.20.255.31")
                port.setText("5555")
                connectButton.visibility = View.VISIBLE
            }
            R.id.connectButton -> {
                if (mWifi.isConnected) {
                    Toast.makeText(applicationContext, "CONECTADO", Toast.LENGTH_SHORT).show()
                    progressBarAction()
                    defaultValuesButton.visibility = View.INVISIBLE
                    addCommandToList("adb tcpip ${port.text}")
                    addCommandToList("adb connect ${server.text}")
                    sendFileButton.visibility = View.VISIBLE
                    killButton.visibility = View.VISIBLE
                    listDevicesButton.visibility = View.VISIBLE

                    crypto = readCryptoConfig(filesDir)
                    setConnection()

                } else
                    Toast.makeText(applicationContext, "CONÉCTESE AL WIFI", Toast.LENGTH_SHORT)
                        .show()
            }
            R.id.sendFileButton -> {
                addCommandToList("adb push patata.txt /sdcard/patata.txt")
            }

            R.id.killButton -> {
                addCommandToList("adb kill-server")
            }

            R.id.listDevicesButton -> {
                addCommandToList("adb devices")
                defaultValuesButton.visibility = View.VISIBLE
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

    private fun setConnection(){

    }

    private fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, PUBLIC_KEY_NAME)
        val privKey = File(dataDir, PRIVATE_KEY_NAME)
        var crypto: AdbCrypto? = null

        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: java.lang.Exception) {
                null
            }
        }
        return crypto
    }

}