package org.zotero.android.screens.share.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.uicomponents.Strings

@AndroidEntryPoint
class ShareUploadService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent == null) return START_NOT_STICKY
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }


        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val chan = NotificationChannel(
            "share-file-upload-notification-id",
            getString(Strings.share_upload_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return "share-file-upload-notification-id"
    }

    companion object {
        fun start(
            context: Context,
        ) {
            val intent = Intent(context, ShareUploadService::class.java).apply {
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShareUploadService::class.java))
        }
    }
}
