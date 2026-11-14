package com.mizaj.sakina

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.widget.TimePicker
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var pendingHour = 20
    private var pendingMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isVerticalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = false
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("mizaj_reminders", "إشعارات مِزاج وسكينة", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "رسائل تذكير يومية وإنجازات المسبحة"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun openReminderSettings() {
        val prefs = getSharedPreferences("reminder", Context.MODE_PRIVATE)
        pendingHour = prefs.getInt("hour", 20)
        pendingMinute = prefs.getInt("minute", 0)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 8) }
        val description = TextView(this).apply { text = "اختر وقت الرسالة اليومية التي تصلك حتى بعد إغلاق التطبيق."; textSize = 15f }
        val enabled = CheckBox(this).apply { text = "تفعيل الإشعار اليومي"; isChecked = prefs.getBoolean("enabled", false) }
        val timeText = TextView(this).apply { textSize = 18f; setPadding(0, 24, 0, 24); text = String.format("وقت الإشعار: %02d:%02d", pendingHour, pendingMinute) }
        val chooseTime = android.widget.Button(this).apply { text = "اختيار الوقت"; setOnClickListener {
            TimePickerDialog(this@MainActivity, { _: TimePicker, h: Int, m: Int ->
                pendingHour = h; pendingMinute = m
                timeText.text = String.format("وقت الإشعار: %02d:%02d", h, m)
            }, pendingHour, pendingMinute, true).show()
        } }
        box.addView(description); box.addView(enabled); box.addView(timeText); box.addView(chooseTime)
        AlertDialog.Builder(this)
            .setTitle("🔔 إشعارات الهاتف")
            .setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                if (enabled.isChecked) {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        prefs.edit().putInt("hour", pendingHour).putInt("minute", pendingMinute).putBoolean("pending", true).apply()
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
                    } else {
                        ReminderScheduler.schedule(this, pendingHour, pendingMinute)
                        prefs.edit().putBoolean("enabled", true).apply()
                        sendStatus("تم حفظ الإشعار اليومي. سيصلك في الوقت المحدد حتى بعد إغلاق التطبيق.")
                    }
                } else {
                    ReminderScheduler.cancel(this)
                    prefs.edit().putBoolean("enabled", false).putBoolean("pending", false).apply()
                    sendStatus("تم إيقاف الإشعار اليومي.")
                }
            }.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501) {
            val prefs = getSharedPreferences("reminder", Context.MODE_PRIVATE)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val h = prefs.getInt("hour", 20); val m = prefs.getInt("minute", 0)
                ReminderScheduler.schedule(this, h, m)
                prefs.edit().putBoolean("enabled", true).putBoolean("pending", false).apply()
                sendStatus("تم تفعيل الإشعار اليومي بنجاح.")
            } else sendStatus("لم يتم منح إذن الإشعارات. يمكنك تفعيله من إعدادات الهاتف.")
        }
    }

    private fun sendStatus(message: String) {
        if (::webView.isInitialized) webView.post { webView.evaluateJavascript("try{localStorage.setItem('ms-notification-status', ${org.json.JSONObject.quote(message)})}catch(e){}", null) }
    }

    inner class AndroidBridge(private val context: Context) {
        @JavascriptInterface fun openReminderSettings() { runOnUiThread { this@MainActivity.openReminderSettings() } }
        @JavascriptInterface fun showAchievement(message: String) { runOnUiThread { NotificationHelper.show(context, "أحسنت! 🌸", message) } }
    }
}
