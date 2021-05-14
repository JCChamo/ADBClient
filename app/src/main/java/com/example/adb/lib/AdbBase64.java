package com.example.adb.lib;

public interface AdbBase64 {

    /**
     * This function must encoded the specified data as a base 64 string, without
     * appending any extra newlines or other characters.
     *
     * @param data Data to encode
     * @return String containing base 64 encoded data
     */
    String encodeToString(byte[] data);

}