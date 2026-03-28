package com.tickmate.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tickmate.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "budget_alert"
        private const val CHANNEL_NAME = "预算提醒"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_LAST_NOTIFY_DATE = "last_notify_date"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "当月消费达到预算阈值时提醒"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    // 检查是否有通知权限
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // 发送预算提醒通知（每天最多一次）
    fun sendBudgetAlert(currentTotal: Double, budget: Double, percent: Int) {
        // 检查今天是否已经发送过
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastDate = prefs.getString(KEY_LAST_NOTIFY_DATE, "")
        if (lastDate == today) return

        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("预算提醒")
            .setContentText("您本月消费已达预算的${percent}%（¥${"%.2f".format(currentTotal)}/¥${"%.2f".format(budget)}）")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

        // 记录发送日期
        prefs.edit().putString(KEY_LAST_NOTIFY_DATE, today).apply()
    }
}
