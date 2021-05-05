package com.example.adb.interfaces

import android.util.Base64
import com.cgutman.adblib.AdbBase64

open class AndroidBase64 : AdbBase64 {
    override fun encodeToString(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }
}