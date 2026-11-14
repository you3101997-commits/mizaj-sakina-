package com.mizaj.sakina

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val UNIQUE = "mizaj-daily-reminder"
    fun schedule(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)
        prefs.edit().putInt("hour", hour).putInt("minute", minute).putBoolean("enabled", true).apply()
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (!after(now)) add(Calendar.DAY_OF_YEAR, 1) }
        val delay = (next.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putInt("hour", hour).putInt("minute", minute).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.REPLACE, request)
    }
    fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(UNIQUE) }
}
