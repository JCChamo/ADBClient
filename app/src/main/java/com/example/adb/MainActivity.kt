package com.example.adb

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Base64.encodeToString
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.adb.lib.AdbBase64
import com.example.adb.lib.AdbConnection
import com.example.adb.lib.AdbCrypto
import com.example.adb.lib.AdbStream
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var defaultValuesButton: Button
    private lateinit var connectButton: Button
    private lateinit var rebootButton: Button
    private lateinit var killButton: Button
    private lateinit var pushApkButton: Button
    private lateinit var installApkButton: Button
    private lateinit var pullApkButton: Button
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

    private var uri : Uri? = null
    private lateinit var file : File
    private lateinit var path : String
    private lateinit var remotePath : String
    private lateinit var fileName : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultValuesButton = findViewById(R.id.defaultValuesButton)
        connectButton = findViewById(R.id.connectButton)
        rebootButton = findViewById(R.id.rebootButton)
        killButton = findViewById(R.id.killButton)
        pushApkButton = findViewById(R.id.pushApkButton)
        installApkButton = findViewById(R.id.installApkButton)
        pullApkButton = findViewById(R.id.pullApkButton)
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
        pushApkButton.setOnClickListener(this)
        installApkButton.setOnClickListener(this)
        pullApkButton.setOnClickListener(this)
        installApkButton.isClickable = false


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


        val message = "adb devices"
        val messageByteArray = message.toByteArray(Charsets.UTF_8)
//        AdbProtocol.generateMessage()

    }

    override fun onResume() {
        super.onResume()
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.defaultValuesButton -> {
//                ip.setText("172.20.255.31")
//                ip.setText("192.168.1.91")
                ip.setText("192.168.1.184")
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
                    pushApkButton.visibility = View.VISIBLE
                    installApkButton.visibility = View.VISIBLE
                    pullApkButton.visibility = View.VISIBLE
                    startConnection()
                } else
                    Toast.makeText(applicationContext, "CONÉCTESE AL WIFI", Toast.LENGTH_SHORT)
                        .show()
            }
            R.id.rebootButton -> {
//                addCommandToList("adb push patata.txt /sdcard/patata.txt")
                addCommandToList("adb reboot")
                sendCommand("shell:","reboot")
            }

            R.id.killButton -> {
                addCommandToList("adb kill-server")
                disconnect()
            }

            R.id.pushApkButton -> {
                openFileExplorer()
                Handler().postDelayed({
                    addCommandToList("adb push")
                    sendCommand("sync:", "", ByteArray(connection.maxData))
                }, 9000)
                installApkButton.isClickable = true
            }

            R.id.pullApkButton -> {
                addCommandToList("adb pull")
                sendCommand("sync:", "", ByteArray(connection.maxData))
                installApkButton.isClickable = true
            }

            R.id.installApkButton -> {
                addCommandToList("adb install")
                sendCommand("shell:pm install -r $remotePath", "")
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

    @SuppressLint("SdCardPath")
    private fun sendCommand(destination: String, command: String = "", byteArray: ByteArray? = null) {
        Thread(Runnable {
            openStream(destination)
            try {
                if (command.contains("install"))
                    Log.d(":::", "Instalando")
                else if (byteArray == null)
                    stream.write(command + '\n')
                else if (commandList[commandList.size - 1] == "adb push"){

                    Log.d(":::", "Dentro del else")
//                    remotePath = "data/local/tmp/prueba.apk"
                    remotePath = "data/local/tmp/$fileName"
                    val mode = ",33206"
                    val length = ("$remotePath$mode").length

                    stream.write(ByteUtils.concat("SEND".toByteArray(), ByteUtils.intToByteArray(length)))
                    stream.write(remotePath.toByteArray())
                    stream.write(mode.toByteArray())

                    val inputStream = FileInputStream(file.path)
                    Log.d(":::", "FileInputStream inicializado")
//                    stream.write(byteArray)

                    while (true) {
                        Log.d(":::", "Dentro del while")
                        val read = inputStream.read(byteArray)
                        if (read < 0) {
                            Log.d(":::", "Read < 0")
                            break
                        }
                        Log.d(":::", "READ: $read")
                        stream.write(ByteUtils.concat("DATA".toByteArray(), ByteUtils.intToByteArray(read)))
                        if (read == byteArray.size) {
                            Log.d(":::", "BYTEARRAY.SIZE: ${byteArray.size}")
                            stream.write(byteArray)
                        } else {
                            val tmp = ByteArray(read)
                            Log.d(":::", "TEMP: $tmp")
                            System.arraycopy(byteArray, 0, tmp, 0, read)
                            stream.write(tmp)
                        }
                    }
                    Log.d(":::", "Fuera del while")
                    stream.write(ByteUtils.concat("DONE".toByteArray(), ByteUtils.intToByteArray(System.currentTimeMillis().toInt())))
                    Log.d(":::", "HECHO")
                    val res = stream.read()
                    file.delete()
                    Log.d(":::", "RES: $res")
                    stream.write(ByteUtils.concat("QUIT".toByteArray(), ByteUtils.intToByteArray(0)))
                    Log.d(":::", "SALIR")
                }

                else if (commandList[commandList.size - 1] == "adb pull"){
                    val remoteFile = "/data/local/tmp/imagen.PNG"
//                    val remoteFile = "/data/local/tmp/prueba_petroprix.jpg"
                    val localFile = "/sdcard/Android/data/com.example.pruebastream/files/imagen.PNG"

//                    stream.write(remoteFile.toByteArray())
                    stream.write("RECV".toByteArray())
                    stream.write(ByteUtils.intToByteArray(remoteFile.length))
                    stream.write(remoteFile.toByteArray())

                    val outputStream = FileOutputStream(localFile)
                    Log.d(":::", "FileOutputStream inicializado")

                    while (true) {
                        var read = stream.read()

                        var readString = String(read, StandardCharsets.UTF_8)
                        Log.d(":::ORIGINAL", readString)
                        if (readString.contains("DATA")) {
                            readString = readString.replace("DATA", "")
                            Log.d(":::SIN DATA", readString)
                        }

                        if (readString.contains("DONE")) {
                            readString = readString.replace("DONE", "")
                            Log.d(":::SIN DONE", readString)
                            outputStream.write(readString.toByteArray())
                            stream.write("OKAY".toByteArray())
                            break
                        }
                        Log.d(":::LIMPIA", readString)
                        outputStream.write(readString.toByteArray())
                        stream.write("OKAY".toByteArray())
                    }
//                    while (true) {
//                        Log.d(":::", "Dentro del while")
//                        val read = stream.read()
//                        if (read.size < 0) {
//                            Log.d(":::", "Read < 0")
//                            break
//                        }
//                        if (read.size == byteArray.size) {
//                            Log.d(":::", "BYTEARRAY.SIZE: ${read.size}")
//                            outputStream.write(read)
//                        } else {
//                            val tmp = ByteArray(read.size)
//                            System.arraycopy(byteArray, 0, tmp, 0, read.size)
//                            outputStream.write(tmp)
//                        }
//                    }
                    stream.write(ByteUtils.concat("QUIT".toByteArray(), ByteUtils.intToByteArray(0)))
                    Log.d(":::", "SALIR")
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.d(":::", "Comando enviado")
        }).start()
    }

    private fun openStream(destination : String) {
        try {
            stream = connection.open(destination)
        } catch (e : java.lang.Exception) {
            e.printStackTrace()
        }
        Log.d(":::", "Flujo abierto")
    }

    private fun getResponse(){
        try {
            val responseBytes = stream.read()
            runOnUiThread{
                if (responseBytes != null)
                    addCommandToList(String(responseBytes, StandardCharsets.UTF_8))
                else
                    addCommandToList("Respuesta nula")
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        Log.d(":::", "Respuesta recibida")
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

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            uri = data?.data
            fileName = getFileName(applicationContext.contentResolver, uri!!)!!
            file = createTempFile(fileName.split(".")[0], ".${fileName.split(".")[1]}")
            Log.d(":::TEMP PATH", file.path)
            path = uri!!.path!!
            val stream = contentResolver.openInputStream(uri!!)
            IOUtils.copy(stream, FileOutputStream(file))
            Log.d(":::PATH", path)
            Log.d(":::NAME", fileName)
        }
    }

    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        val returnCursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

}