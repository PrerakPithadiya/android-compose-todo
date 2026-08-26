package com.example.todo_list.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todo_list.MainActivity

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val taskCategory = intent.getStringExtra("TASK_CATEGORY") ?: "General"
        val taskTime = intent.getStringExtra("TASK_TIME") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity when the user taps the notification
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // System Blue Accent color matching iOS guidelines (#007AFF)
        val systemBlueColor = 0xFF007AFF.toInt()

        val notificationBuilder = NotificationCompat.Builder(context, TaskNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(taskTitle)
            .setContentText("Reminder for your $taskCategory task at $taskTime")
            .setColor(systemBlueColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(taskId.hashCode(), notificationBuilder.build())
    }
}
