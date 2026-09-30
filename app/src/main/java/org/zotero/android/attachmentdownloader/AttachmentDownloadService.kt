package org.zotero.android.attachmentdownloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import javax.inject.Inject

// Keeps batch attachment downloads running while the app is in the background, and shows their
// progress in a silent notification that can cancel them
@AndroidEntryPoint
class AttachmentDownloadService : Service() {

    @Inject
    lateinit var attachmentDownloader: AttachmentDownloader

    @Inject
    lateinit var dispatchers: Dispatchers

    private var coroutineScope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            attachmentDownloader.stop()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        // Must be called for every start, even if the batch has already finished
        val batchState = attachmentDownloader.batchState.value
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(batchState),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
        observeBatchState()
        return START_NOT_STICKY
    }

    private fun observeBatchState() {
        if (coroutineScope != null) {
            return
        }
        val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
        coroutineScope = scope
        scope.launch {
            attachmentDownloader.batchState.collect { batchState ->
                if (batchState == null) {
                    ServiceCompat.stopForeground(
                        this@AttachmentDownloadService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE
                    )
                    stopSelf()
                } else {
                    updateNotification(batchState)
                }
            }
        }
    }

    private fun updateNotification(batchState: AttachmentDownloader.BatchState) {
        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        try {
            notificationManager.notify(NOTIFICATION_ID, createNotification(batchState))
        } catch (e: SecurityException) {
            Timber.w(e, "AttachmentDownloadService: can't post notification")
        }
    }

    private fun createNotification(batchState: AttachmentDownloader.BatchState?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(Drawables.download_24px)
            .setContentTitle(getString(Strings.attachment_downloads_notification_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(createOpenAppIntent())
            .addAction(0, getString(Strings.cancel), createCancelIntent())

        when (batchState) {
            is AttachmentDownloader.BatchState.Downloading -> {
                builder
                    .setContentText(
                        getString(Strings.items_toolbar_downloaded, batchState.downloaded, batchState.total)
                    )
                    .setProgress(batchState.total, batchState.downloaded, false)
            }
            AttachmentDownloader.BatchState.Preparing, null -> {
                builder.setContentText(getString(Strings.all_items_preparing_downloads))
            }
        }
        return builder.build()
    }

    private fun createOpenAppIntent(): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createCancelIntent(): PendingIntent {
        val intent = Intent(this, AttachmentDownloadService::class.java).setAction(ACTION_CANCEL)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        // Low importance: no sound, vibration or heads-up popup
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(Strings.attachment_downloads_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Data sync foreground services are limited to 6 hours a day on Android 15. Downloads continue
    // while the app is in use, only the notification and background protection end
    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.w("AttachmentDownloadService: foreground service timed out")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        coroutineScope?.cancel()
        coroutineScope = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "attachment-downloads"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_CANCEL = "org.zotero.android.attachmentdownloader.CANCEL"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AttachmentDownloadService::class.java)
                )
            } catch (e: Exception) {
                // Starting a foreground service isn't allowed while the app is in the background.
                // Batches are started by the user, so downloads just continue without the notification
                Timber.w(e, "AttachmentDownloadService: can't start")
            }
        }
    }
}
