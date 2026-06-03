package com.example.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ServerTimeService {
    private val client = OkHttpClient.Builder().build()
    private var timeOffset: Long = 0L
    private var isSynced: Boolean = false

    fun getOffset() = timeOffset
    fun isTimeSynced() = isSynced

    suspend fun syncTime(): Long = withContext(Dispatchers.IO) {
        // Option 1: fetch from Google HTTP Headers (highest uptime, extremely reliable and lightweight)
        try {
            val request = Request.Builder()
                .url("https://www.google.com")
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                val dateHeader = response.header("Date")
                if (dateHeader != null) {
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                    val serverDate = sdf.parse(dateHeader)
                    if (serverDate != null) {
                        val serverTime = serverDate.time
                        val systemTime = System.currentTimeMillis()
                        timeOffset = serverTime - systemTime
                        isSynced = true
                        Log.d("ServerTimeService", "Synced via Google: offset=$timeOffset ms")
                        return@withContext serverTime
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ServerTimeService", "Google header sync failed: ${e.message}")
        }

        // Option 2 fallback: Try WorldTimeAPI
        try {
            val request = Request.Builder()
                .url("http://worldtimeapi.org/api/timezone/Asia/Kuala_Lumpur")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val regex = "\"unixtime\":(\\d+)".toRegex()
                        val match = regex.find(bodyString)
                        if (match != null) {
                            val unixTimeSec = match.groupValues[1].toLong()
                            val serverTimeMs = unixTimeSec * 1000L
                            val systemTime = System.currentTimeMillis()
                            timeOffset = serverTimeMs - systemTime
                            isSynced = true
                            Log.d("ServerTimeService", "Synced via WorldTimeAPI: offset=$timeOffset ms")
                            return@withContext serverTimeMs
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ServerTimeService", "Fallback WorldTimeAPI sync failed: ${e.message}")
        }

        // Unsynced system fallback
        isSynced = false
        return@withContext System.currentTimeMillis()
    }

    // Get current synchronized server epoch millis
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis() + timeOffset
    }

    // Format millisecond timestamp into Malaysia Standard Time
    fun formatToMalaysiaTime(timestampMs: Long, pattern: String = "hh:mm a"): String {
        val date = Date(timestampMs)
        val sdf = SimpleDateFormat(pattern, Locale("ms", "MY"))
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        return sdf.format(date)
    }

    fun formatToMalaysiaDate(timestampMs: Long, pattern: String = "yyyy-MM-dd"): String {
        val date = Date(timestampMs)
        val sdf = SimpleDateFormat(pattern, Locale("ms", "MY"))
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        return sdf.format(date)
    }
}
