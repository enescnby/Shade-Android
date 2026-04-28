package com.shade.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shade.app.MainActivity
import com.shade.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID = "shade_messages"
        private const val CHANNEL_NAME = "Messages"
        private const val GROUP_KEY = "shade_messages_group"
        private const val SUMMARY_ID = 0
    }

    private val messageByUser = ConcurrentHashMap<String, MutableList<String>>()

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat messages from Shade"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showMessageNotification(senderName: String, message: String, senderShadeId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messages = messageByUser.getOrPut(senderShadeId) { mutableListOf() }
        messages.add(message)

        val notificationId = senderShadeId.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("chatId", senderShadeId)
            putExtra("chatName", senderName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(senderName)

        messages.takeLast(7).forEach { msg ->
            inboxStyle.addLine(msg)
        }

        if (messages.size > 7) {
            inboxStyle.setSummaryText("+${messages.size - 7} daha fazla mesaj")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(senderName)
            .setContentText(message)
            .setSmallIcon(R.drawable.shade_logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY)
            .setStyle(inboxStyle)
            .setNumber(messages.size)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)

        if (messageByUser.size > 1) {
            val totalMessages = messageByUser.values.sumOf { it.size }

            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Shade")
                .setContentText("${messageByUser.size} sohbetten $totalMessages mesaj")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            manager.notify(SUMMARY_ID, summaryNotification)
        }
    }

    fun clearNotifications(senderShadeId: String) {
        messageByUser.remove(senderShadeId)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(senderShadeId.hashCode())

        if (messageByUser.size <= 1) {
            manager.cancel(SUMMARY_ID)
        }
    }
}
