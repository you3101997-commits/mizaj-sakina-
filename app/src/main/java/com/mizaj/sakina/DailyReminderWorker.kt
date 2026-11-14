package com.mizaj.sakina

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("reminder", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return Result.success()
        NotificationHelper.show(applicationContext, "طمني عنك 🤍", "كيف حال قلبك اليوم؟ خذ لحظة هدوء واذكر الله.")
        ReminderScheduler.schedule(applicationContext, inputData.getInt("hour", prefs.getInt("hour", 20)), inputData.getInt("minute", prefs.getInt("minute", 0)))
        return Result.success()
    }
}
