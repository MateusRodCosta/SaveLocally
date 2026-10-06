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

package com.mateusrodcosta.apps.share2storage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import com.mateusrodcosta.apps.share2storage.domain.usecases.SaveFileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicInteger

class SaveLocallyService: Service(), KoinComponent {

    private val saveFileUseCase: SaveFileUseCase by inject()

    companion object {

        private const val PROGRESS_CHANNEL_ID = "save_locally_in_progress_channel"
        private const val RESULT_CHANNEL_ID = "save_locally_finished_channel"
        private const val PRIMARY_FOREGROUND_ID = 1000

        const val ACTION_SAVE_TEXT = "ACTION_SAVE_TEXT"
        const val ACTION_SAVE_FILE = "ACTION_SAVE_FILE"

        const val EXTRA_TEXT = "text"
        const val EXTRA_SOURCE_URI = "sourceUri"
        const val EXTRA_TARGET_URI = "targetUri"

        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_FILE_SIZE = "fileSize"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobsCount = AtomicInteger(0)

    inner class LocalBinder : Binder() {
        fun getService(): SaveLocallyService = this@SaveLocallyService
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return

        val progressChannel = NotificationChannel(
            PROGRESS_CHANNEL_ID,
            getString(R.string.saving_file),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing file saving progress channel"
        }

        val resultChannel = NotificationChannel(
            RESULT_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "File save completion results channel"
        }

        notificationManager.createNotificationChannels(listOf(progressChannel, resultChannel))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notificationId = startId + PRIMARY_FOREGROUND_ID
        val activeJobs = activeJobsCount.incrementAndGet()

        val progressNotification = NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.saving_file))
            .setSmallIcon(R.drawable.ic_notification)
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

        serviceScope.launch {
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

                                val progressNotification =
                                    NotificationCompat.Builder(this@SaveLocallyService, PROGRESS_CHANNEL_ID)
                                        .setContentTitle(fileName)
                                        .setContentText("$copiedStr / $totalStr")
                                        .setSmallIcon(R.drawable.ic_notification)
                                        .setProgress(100, percent, false)
                                        .setOngoing(true)
                                        .build()

                                notificationManager?.notify(notificationId, progressNotification)
                            }
                        }
                    } else {
                        Result.failure(IllegalArgumentException("Missing required EXTRA_SOURCE_URI, EXTRA_TARGET_URI, EXTRA_FILE_NAME, or EXTRA_FILE_SIZE"))
                    }
                }
                else -> null
            }

            result?.let { saveResult ->
                val messageRes = if (saveResult.isSuccess) {
                    R.string.toast_saved_file_success
                } else {
                    R.string.toast_saved_file_failure
                }

                val completedNotification = NotificationCompat.Builder(this@SaveLocallyService, RESULT_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(messageRes))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setAutoCancel(true)
                    .build()

                notificationManager?.notify(notificationId, completedNotification)
            }

            val remainingJobs = activeJobsCount.decrementAndGet()
            if (remainingJobs <= 0) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }

            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder
}