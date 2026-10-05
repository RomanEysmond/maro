package com.maro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maro.MainActivity
import com.maro.R
import com.maro.feature.chat.domain.IncomingMessage

/** System notifications about new messages: one per chat (a newer message replaces the older one). */
class MessageNotifications(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** [message] is `null` when the message could not be fetched in time: then only "new message" is shown. */
    fun show(chatId: String, message: IncomingMessage?) {
        val manager = NotificationManagerCompat.from(context)
        // Android 13+: nothing to do until the user allows notifications.
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(message?.title() ?: context.getString(R.string.app_name))
            .setContentText(message?.body() ?: context.getString(R.string.notification_new_message))
            .setStyle(message?.let { NotificationCompat.BigTextStyle().bigText(it.body()) })
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openChatIntent(chatId))
            .build()
        try {
            manager.notify(chatId, NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // The permission was revoked between the check and the call.
        }
    }

    // A group's notification is the group's: its title, and the sender in front of the text.
    private fun IncomingMessage.title(): String? = (groupTitle ?: senderName).takeIf { it.isNotBlank() }

    private fun IncomingMessage.body(): String =
        if (groupTitle != null && senderName.isNotBlank()) "$senderName: $text" else text

    fun cancel(chatId: String) {
        NotificationManagerCompat.from(context).cancel(chatId, NOTIFICATION_ID)
    }

    private fun openChatIntent(chatId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_CHAT_ID, chatId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_ID = "messages"
        const val NOTIFICATION_ID = 1
    }
}
