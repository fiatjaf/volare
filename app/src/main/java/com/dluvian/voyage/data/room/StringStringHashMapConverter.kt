package com.dluvian.voyage.data.room

import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.room.TypeConverter

class StringStringHashMapConverter {
    private val DIVIDER = "~🪴~"

    @TypeConverter
    fun fromMap(map: Map<String, String>?): String? {
        if (map == null || map.size == 0) return null

        val res = StringBuilder()

        for ((k, v) in map) {
            res.append(k)
            res.append(DIVIDER)
            res.append(v)
            res.append(DIVIDER)
        }

        res.setLength(res.length - DIVIDER.length)

        return res.toString()
    }

    @TypeConverter
    fun toMap(data: String?): Map<String, String>? {
        if (data == null || data.length == 0) return null

        val map = mutableMapOf<String, String>()

        val spl = data.split(DIVIDER)
        var i = 0
        while (i < spl.size) {
            map[spl[i]] = spl[i + 1]
            i += 2
        }

        return map
    }
}
