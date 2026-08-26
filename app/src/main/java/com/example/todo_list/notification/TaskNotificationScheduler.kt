package com.example.todo_list.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.todo_list.model.TaskItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskNotificationScheduler {
    const val CHANNEL_ID = "task_notifications_channel"
    private const val TAG = "TaskNotificationScheduler"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminders"
            val descriptionText = "Notifications for scheduled tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, task: TaskItem) {
        if (task.isCompleted) {
            cancel(context, task)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = getTaskCalendar(task)

        // If the task scheduled date/time is in the past, do not schedule
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Not scheduling task ${task.id} because its scheduled time ${calendar.time} is in the past")
            return
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_CATEGORY", task.category)
            putExtra("TASK_TIME", task.time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "Scheduling task ${task.id} for time ${calendar.time} (millis: ${calendar.timeInMillis})")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            Log.d(TAG, "Cancelling scheduled notification for task ${task.id}")
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun getTaskCalendar(task: TaskItem): Calendar {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        when (task.date) {
            "Today" -> {
                calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            }
            "Tomorrow" -> {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                calendar.set(Calendar.YEAR, tomorrow.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, tomorrow.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, tomorrow.get(Calendar.DAY_OF_MONTH))
            }
            else -> {
                try {
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val parsedDate = formatter.parse(task.date)
                    if (parsedDate != null) {
                        val parsedCal = Calendar.getInstance().apply { time = parsedDate }
                        calendar.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR))
                        calendar.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH))
                        calendar.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH))
                    } else {
                        throw Exception("Null parsed date")
                    }
                } catch (e: Exception) {
                    val dateFromEpoch = Date(task.epochDay * 24 * 60 * 60 * 1000)
                    val parsedCal = Calendar.getInstance().apply { time = dateFromEpoch }
                    calendar.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR))
                    calendar.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH))
                    calendar.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }

        try {
            val parts = task.time.trim().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"

            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (amPm == "PM") {
                calendar.set(Calendar.HOUR_OF_DAY, if (hour == 12) 12 else hour + 12)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, if (hour == 12) 0 else hour)
            }
        } catch (e: Exception) {
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        return calendar
    }
}
