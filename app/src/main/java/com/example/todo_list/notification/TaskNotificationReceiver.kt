package com.example.todo_list.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todo_list.manager.AppIconManager

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val taskCategory = intent.getStringExtra("TASK_CATEGORY") ?: "General"
        val taskTime = intent.getStringExtra("TASK_TIME") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Dynamically resolve the active AppIcon
        val activeIcon = AppIconManager.getActiveIcon(context)

        TaskNotificationScheduler.createNotificationChannel(context)

        // Launch the active activity alias with launcher flags
        val clickIntent = Intent().apply {
            component = ComponentName(context.packageName, activeIcon.aliasClassName)
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TaskNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(activeIcon.iconResId)
            .setContentTitle(taskTitle)
            .setContentText("Reminder for your $taskCategory task at $taskTime")
            .setColor(activeIcon.accentColorInt)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
