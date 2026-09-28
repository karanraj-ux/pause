package com.example.shield

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.ShieldApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledTaskWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getInt("taskId", -1)
        if (taskId == -1) return@withContext Result.failure()

        val repo = (applicationContext as ShieldApplication).container.scheduledTaskRepository
        val task = repo.getTaskById(taskId) ?: return@withContext Result.failure()

        if (task.completed) return@withContext Result.success()

        try {
            when (task.type) {
                "SMS" -> {
                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        applicationContext.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION") SmsManager.getDefault()
                    }
                    val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
                    val rawContent = task.message ?: ""
                    val content = MessageDispatcherHelper.resolveDynamicPlaceholders(
                        context = applicationContext,
                        template = rawContent,
                        senderName = task.target,
                        senderNumber = task.target,
                        settingsRepo = settingsRepo
                    )
                    val parts = smsManager.divideMessage(content)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(task.target, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(task.target, null, content, null, null)
                    }
                    Log.d("ScheduledTaskWorker", "Sent scheduled SMS to ${task.target}")
                }
                "Call" -> {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.parse("tel:${task.target}")
                    showTapToLaunchNotification(applicationContext, "Scheduled Call", "Tap to call ${task.target}", intent, taskId)
                    Log.d("ScheduledTaskWorker", "Posted call notification for ${task.target}")
                }

                "Ghost Mode", "Silent Guard" -> {
                    val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
                    settingsRepo.updateBoolean(com.example.data.repository.SettingsRepository.GHOST_MODE, true)
                    Log.d("ScheduledTaskWorker", "Activated Silent Guard via schedule")
                }
                "WhatsApp" -> {
                    val rawMsg = task.message ?: ""
                    val mediaUri = extractMediaUri(rawMsg)
                    val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
                    val rawText = extractCleanText(rawMsg)
                    val cleanText = MessageDispatcherHelper.resolveDynamicPlaceholders(
                        context = applicationContext,
                        template = rawText,
                        senderName = task.target,
                        senderNumber = task.target,
                        settingsRepo = settingsRepo
                    )
                    val cleanNumber = task.target.replace(Regex("[^0-9]"), "")
                    
                    val intent = if (mediaUri != null) {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, mediaUri)
                            if (cleanNumber.isNotEmpty()) {
                                putExtra("jid", "$cleanNumber@s.whatsapp.net")
                            }
                            if (cleanText.isNotEmpty()) {
                                putExtra(Intent.EXTRA_TEXT, cleanText)
                            }
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } else {
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(cleanText)}")
                        }
                    }
                    showTapToLaunchNotification(
                        applicationContext,
                        "Scheduled WhatsApp Message",
                        if (mediaUri != null) "Tap to send image & message to ${task.target}" else "Tap to send message to ${task.target}",
                        intent,
                        taskId
                    )
                    Log.d("ScheduledTaskWorker", "Posted WhatsApp notification for ${task.target} (hasMedia=${mediaUri != null})")
                }
            }

            
            if (task.isRecurring && task.recurringIntervalMillis > 0) {
                var nextTimeMillis = task.timeMillis + task.recurringIntervalMillis
                val now = System.currentTimeMillis()
                while (nextTimeMillis <= now) {
                    nextTimeMillis += task.recurringIntervalMillis
                }
                val nextTask = task.copy(timeMillis = nextTimeMillis, completed = false)
                repo.updateTask(nextTask)
                
                val delay = nextTimeMillis - System.currentTimeMillis()
                if (delay > 0) {
                    val workRequest = androidx.work.OneTimeWorkRequestBuilder<ScheduledTaskWorker>()
                        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .setInputData(androidx.work.Data.Builder().putInt("taskId", task.id).build())
                        .build()
                    androidx.work.WorkManager.getInstance(applicationContext).enqueue(workRequest)
                }
            } else {
                repo.markCompleted(taskId)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduledTaskWorker", "Failed to execute task", e)
            Result.failure()
        }
    }

    private fun showTapToLaunchNotification(context: Context, title: String, content: String, intent: Intent, notificationId: Int) {
        val channelId = "scheduled_tasks"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Scheduled Tasks", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    companion object {
        fun extractMediaUri(message: String?): Uri? {
            if (message.isNullOrBlank()) return null
            val regex = Regex("""\[(?:Image|Media|File):\s*(content://[^\s\]]+)\]""")
            val match = regex.find(message)
            return match?.groupValues?.get(1)?.let { Uri.parse(it) }
        }

        fun extractCleanText(message: String?): String {
            if (message.isNullOrBlank()) return ""
            return message.replace(Regex("""\[(?:Image|Media|File):\s*content://[^\s\]]+\]"""), "").trim()
        }
    }
}
