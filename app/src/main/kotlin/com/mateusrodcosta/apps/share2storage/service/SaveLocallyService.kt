/*
 *     Copyright (C) 2026 Mateus Rodrigues Costa
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mateusrodcosta.apps.share2storage.service

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.domain.exception.InsufficientStorageException
import com.mateusrodcosta.apps.share2storage.domain.usecases.SaveFileUseCase
import com.mateusrodcosta.apps.share2storage.domain.usecases.SaveZippedFilesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SaveLocallyService: Service(), KoinComponent {

    private val saveFileUseCase: SaveFileUseCase by inject()
    private val saveZippedFilesUseCase: SaveZippedFilesUseCase by inject()

    companion object {

        private const val PROGRESS_CHANNEL_ID = "save_locally_in_progress_channel"
        private const val RESULT_CHANNEL_ID = "save_locally_finished_channel"
        private const val PRIMARY_FOREGROUND_ID = 1000

        const val ACTION_SAVE_TEXT = "ACTION_SAVE_TEXT"
        const val ACTION_SAVE_FILE = "ACTION_SAVE_FILE"
        const val ACTION_SAVE_ZIPPED_FILES = "ACTION_SAVE_ZIPPED_FILES"
        const val ACTION_CANCEL_SAVE = "ACTION_CANCEL_SAVE"

        const val EXTRA_TEXT = "text"
        const val EXTRA_SOURCE_URI = "sourceUri"
        const val EXTRA_SOURCE_URIS = "sourceUris"
        const val EXTRA_TARGET_URI = "targetUri"

        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_FILE_NAMES = "fileNames"
        const val EXTRA_FILE_SIZE = "fileSize"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobsCount = AtomicInteger(0)
    private val activeJobsMap = ConcurrentHashMap<Int, Job>()

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return

        val progressChannel = NotificationChannel(
            PROGRESS_CHANNEL_ID,
            getString(R.string.notification_channel_name_in_progress),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_description_in_progress)
        }

        val resultChannel = NotificationChannel(
            RESULT_CHANNEL_ID,
            getString(R.string.notification_channel_name_finished),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_description_finished)
        }

        notificationManager.createNotificationChannels(listOf(progressChannel, resultChannel))
    }

    private fun buildCancelPendingIntent(notificationId: Int): PendingIntent {
        val cancelIntent = Intent(this, SaveLocallyService::class.java).apply {
            action = ACTION_CANCEL_SAVE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getService(
            this,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (intent?.action == ACTION_CANCEL_SAVE) {
            val notificationIdToCancel = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationIdToCancel != -1) {
                val jobToCancel = activeJobsMap.remove(notificationIdToCancel)
                jobToCancel?.cancel()
                notificationManager?.cancel(notificationIdToCancel)
                Toast.makeText(applicationContext, R.string.message_save_cancelled, Toast.LENGTH_SHORT).show()
            }
            return START_NOT_STICKY
        }

        val activeJobs = activeJobsCount.incrementAndGet()
        val notificationId = if (activeJobs == 1) PRIMARY_FOREGROUND_ID else startId + PRIMARY_FOREGROUND_ID

        val initialFileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: getString(R.string.app_name)
        val cancelPendingIntent = buildCancelPendingIntent(notificationId)

        val progressNotification = NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(initialFileName)
            .setContentText(getString(R.string.saving_file))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.cancel_button),
                cancelPendingIntent
            )
            .setOngoing(true)
            .build()

        if (activeJobs == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PRIMARY_FOREGROUND_ID,
                    progressNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(PRIMARY_FOREGROUND_ID, progressNotification)
            }
        } else {
            notificationManager?.notify(notificationId, progressNotification)
        }

        val job = serviceScope.launch {
            try {
                if (setOf(ACTION_SAVE_FILE, ACTION_SAVE_TEXT, ACTION_SAVE_ZIPPED_FILES).contains(intent?.action)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, R.string.saving_file, Toast.LENGTH_SHORT).show()
                    }
                }

                val result: Result<Unit>? = when (intent?.action) {
                    ACTION_SAVE_TEXT -> {
                        val text = intent.getStringExtra(EXTRA_TEXT)
                        val targetUri = intent.getStringExtra(EXTRA_TARGET_URI)
                        if (text != null && targetUri != null) {
                            saveFileUseCase.saveText(text, targetUri)
                        } else Result.failure(IllegalArgumentException("Missing params"))
                    }
                    ACTION_SAVE_FILE -> {
                        val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI)
                        val targetUri = intent.getStringExtra(EXTRA_TARGET_URI)
                        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
                        val fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, -1L).takeIf { it > 0L }

                        if (sourceUri != null && targetUri != null && fileName != null && fileSize != null) {
                            var lastUpdateMs = 0L
                            val updateIntervalMs = 250L

                            saveFileUseCase.saveFile(sourceUri, targetUri, totalBytes = fileSize) { bytesCopied, totalBytes ->
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateMs >= updateIntervalMs || bytesCopied == totalBytes) {
                                    lastUpdateMs = now

                                    val copiedStr = Formatter.formatShortFileSize(
                                        this@SaveLocallyService,
                                        bytesCopied
                                    )
                                    val totalStr = Formatter.formatShortFileSize(
                                        this@SaveLocallyService,
                                        totalBytes
                                    )

                                    val percent = ((bytesCopied * 100) / totalBytes).toInt()

                                    val updatedNotification =
                                        NotificationCompat.Builder(this@SaveLocallyService, PROGRESS_CHANNEL_ID)
                                            .setContentTitle(fileName)
                                            .setContentText("$copiedStr / $totalStr")
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                            .setProgress(100, percent, false)
                                            .addAction(
                                                R.drawable.ic_notification,
                                                getString(R.string.cancel_button),
                                                cancelPendingIntent
                                            )
                                            .setOngoing(true)
                                            .build()

                                    notificationManager?.notify(notificationId, updatedNotification)
                                }
                            }
                        } else {
                            Result.failure(IllegalArgumentException("Missing required EXTRA_SOURCE_URI, EXTRA_TARGET_URI, EXTRA_FILE_NAME, or EXTRA_FILE_SIZE"))
                        }
                    }
                    ACTION_SAVE_ZIPPED_FILES -> {
                        val sourceUris = intent.getStringArrayListExtra(EXTRA_SOURCE_URIS)
                        val fileNames = intent.getStringArrayListExtra(EXTRA_FILE_NAMES)
                        val targetUri = intent.getStringExtra(EXTRA_TARGET_URI)

                        if (!sourceUris.isNullOrEmpty() && !fileNames.isNullOrEmpty() && targetUri != null && sourceUris.size == fileNames.size) {
                            val files = sourceUris.zip(fileNames)
                            var lastUpdateMs = 0L
                            val updateIntervalMs = 250L

                            saveZippedFilesUseCase(files, targetUri) { bytesCopied, totalBytes, currentFileName ->
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateMs >= updateIntervalMs || (totalBytes > 0L && bytesCopied == totalBytes)) {
                                    lastUpdateMs = now

                                    val copiedStr = Formatter.formatShortFileSize(
                                        this@SaveLocallyService,
                                        bytesCopied
                                    )

                                    val progressText = if (totalBytes > 0L) {
                                        val totalStr = Formatter.formatShortFileSize(
                                            this@SaveLocallyService,
                                            totalBytes
                                        )
                                        "$copiedStr / $totalStr — $currentFileName"
                                    } else {
                                        "$copiedStr — $currentFileName"
                                    }

                                    val percent = if (totalBytes > 0L) {
                                        ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    } else 0

                                    val updatedNotification =
                                        NotificationCompat.Builder(this@SaveLocallyService, PROGRESS_CHANNEL_ID)
                                            .setContentTitle(initialFileName)
                                            .setContentText(progressText)
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                            .setProgress(if (totalBytes > 0L) 100 else 0, percent, totalBytes <= 0L)
                                            .addAction(
                                                R.drawable.ic_notification,
                                                getString(R.string.cancel_button),
                                                cancelPendingIntent
                                            )
                                            .setOngoing(true)
                                            .build()

                                    notificationManager?.notify(notificationId, updatedNotification)
                                }
                            }
                        } else {
                            Result.failure(IllegalArgumentException("Missing required params for zipping"))
                        }
                    }
                    else -> null
                }

                result?.let { saveResult ->
                    val exception = saveResult.exceptionOrNull()
                    if (exception is CancellationException) {
                        notificationManager?.cancel(notificationId)
                        return@launch
                    }

                    val isNoSpaceError = exception != null && (
                        exception is InsufficientStorageException ||
                        exception.message?.contains("Insufficient storage space", ignoreCase = true) == true ||
                        exception.message?.contains("ENOSPC", ignoreCase = true) == true ||
                        exception.message?.contains("No space left", ignoreCase = true) == true
                    )

                    val messageText = when {
                        saveResult.isSuccess -> getString(R.string.message_saved_file_success)
                        isNoSpaceError -> getString(R.string.message_saved_file_no_space)
                        else -> getString(R.string.message_saved_file_failure)
                    }

                    val completedFileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: getString(R.string.app_name)
                    val resultNotificationId = notificationId + 10000

                    val completedNotificationBuilder = NotificationCompat.Builder(this@SaveLocallyService, RESULT_CHANNEL_ID)
                        .setContentTitle(completedFileName)
                        .setContentText(messageText)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)

                    val targetUriString = intent?.getStringExtra(EXTRA_TARGET_URI)
                    if (saveResult.isSuccess && targetUriString != null) {
                        val targetUri = targetUriString.toUri()
                        val mimeType = contentResolver.getType(targetUri) ?: "*/*"

                        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(targetUri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        val openFilePendingIntent = PendingIntent.getActivity(
                            this@SaveLocallyService,
                            resultNotificationId,
                            openFileIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val docId = runCatching {
                            if (DocumentsContract.isDocumentUri(this@SaveLocallyService, targetUri)) {
                                DocumentsContract.getDocumentId(targetUri)
                            } else null
                        }.getOrNull()

                        val openFolderIntent = if (docId != null && docId.contains('/')) {
                            val parentDocId = docId.substringBeforeLast('/')
                            val authority = targetUri.authority ?: "com.android.externalstorage.documents"
                            val parentUri = DocumentsContract.buildDocumentUri(authority, parentDocId)
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(parentUri, DocumentsContract.Document.MIME_TYPE_DIR)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        } else if (docId != null && docId.contains(':')) {
                            val volume = docId.substringBefore(':')
                            val authority = targetUri.authority ?: "com.android.externalstorage.documents"
                            val parentUri = DocumentsContract.buildDocumentUri(authority, "$volume:")
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(parentUri, DocumentsContract.Document.MIME_TYPE_DIR)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        } else {
                            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        }

                        val openFolderPendingIntent = PendingIntent.getActivity(
                            this@SaveLocallyService,
                            resultNotificationId + 1,
                            openFolderIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        completedNotificationBuilder
                            .setContentIntent(openFilePendingIntent)
                            .addAction(
                                R.drawable.ic_notification,
                                getString(R.string.open_file_button),
                                openFilePendingIntent
                            )
                            .addAction(
                                R.drawable.ic_notification,
                                getString(R.string.open_location_button),
                                openFolderPendingIntent
                            )
                    }

                    val completedNotification = completedNotificationBuilder.build()
                    notificationManager?.notify(resultNotificationId, completedNotification)
                    notificationManager?.cancel(notificationId)
                }
            } finally {
                activeJobsMap.remove(notificationId)
                val remainingJobs = activeJobsCount.decrementAndGet()
                if (remainingJobs <= 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationManager?.cancel(PRIMARY_FOREGROUND_ID)
                }
                stopSelf(startId)
            }
        }

        activeJobsMap[notificationId] = job

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null
}
