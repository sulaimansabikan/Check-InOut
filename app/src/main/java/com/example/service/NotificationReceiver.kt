package com.example.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Alarm broadcast received!")
        
        // Launch in background scope to access database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = Repository(db)
                
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                
                // Fetch all workers and check who hasn't checked in
                val workers = repository.allWorkers.firstOrNull() ?: emptyList()
                val todayLogs = repository.allLogs.firstOrNull()?.filter { it.date == todayDate } ?: emptyList()
                
                val checkedInWorkerIds = todayLogs.filter { it.punchInTime != null }.map { it.workerId }
                val absentWorkers = workers.filter { it.id !in checkedInWorkerIds }
                
                if (absentWorkers.isNotEmpty()) {
                    val names = absentWorkers.joinToString(", ") { it.name }
                    sendAbsentNotification(context, "Kakitangan Belum Punch In!", "Peringatan: Pekerja ($names) belum daftar masuk kerja hari ini.")
                } else {
                    Log.d("NotificationReceiver", "Semua pekerja sudah punch in. Tiada peringatan dikeluarkan.")
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Gagal hantar notifikasi automatik: ${e.message}")
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "punch_card_reminders"
        private const val CHANNEL_NAME = "Peringatan Punch Card"
        private const val NOTIFICATION_ID = 888

        // Post a direct warning/reminder notification
        @SuppressLint("MissingPermission")
        fun sendAbsentNotification(context: Context, title: String, content: String) {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val descriptionText = "Saluran untuk peringatan punch card harian bagi pekerja."
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = descriptionText
                }
                
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Schedule Daily alarm at 07:45 AM
        fun scheduleDailyReminder(context: Context, reminderTimeString: String) { // format HH:mm, e.g. "07:45"
            try {
                val parts = reminderTimeString.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 45
                
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    
                    // If time is in past, add 1 day to run tomorrow morning
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    action = "com.example.ACTION_PUNCH_REMINDER"
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 101, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (alarmManager != null) {
                    // Repeat weekly or daily
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    Log.d("NotificationReceiver", "Scheduled daily alarm at $hour:$minute")
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Gagal jadualkan penggera: ${e.message}")
            }
        }

        fun cancelReminder(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 101, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (alarmManager != null && pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    Log.d("NotificationReceiver", "Cancelled alarm.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
