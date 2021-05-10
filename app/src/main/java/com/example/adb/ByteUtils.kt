package com.example.adb

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteUtils {
    fun concat(vararg arrays: ByteArray): ByteArray {
        // Determine the length of the result array
        var totalLength = 0
        for (i in arrays.indices) {
            totalLength += arrays[i].size
        }

        // create the result array
        val result = ByteArray(totalLength)

        // copy the source arrays into the result array
        var currentIndex = 0
        for (i in arrays.indices) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].size)
            currentIndex += arrays[i].size
        }
        return result
    }

    fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }
}