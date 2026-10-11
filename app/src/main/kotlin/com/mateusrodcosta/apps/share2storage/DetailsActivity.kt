/*
 *     Copyright (C) 2022 - 2026 Mateus Rodrigues Costa
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

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.mateusrodcosta.apps.share2storage.core.AppConstants
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreen
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreenSavingFileFeedback
import com.mateusrodcosta.apps.share2storage.screens.DetailsViewModel
import com.mateusrodcosta.apps.share2storage.screens.MultiDetailsScreen
import com.mateusrodcosta.apps.share2storage.service.SaveLocallyService
import com.mateusrodcosta.apps.share2storage.utils.result.CreateDocumentWithInitialUri
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailsActivity : ComponentActivity() {

    private val viewModel: DetailsViewModel by viewModel()

    private val createFileLauncher: ActivityResultLauncher<CreateDocumentWithInitialUri.Input> =
        registerForActivityResult(CreateDocumentWithInitialUri()) { uri ->
            uri?.let { targetUri ->
                if (sharedContent != null) {
                    val intent = Intent(this, SaveLocallyService::class.java).apply {
                        action = SaveLocallyService.ACTION_SAVE_TEXT
                        putExtra(SaveLocallyService.EXTRA_TEXT, sharedContent.toString())
                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUri.toString())
                    }

                    ContextCompat.startForegroundService(this, intent)
                    finish()
                } else {
                    val intent = Intent(this, SaveLocallyService::class.java).apply {
                        action = SaveLocallyService.ACTION_SAVE_FILE
                        putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceFileUri.toString())
                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUri.toString())

                        val data = checkNotNull(viewModel.uriData.value) { "UriData metadata must be loaded before saving" }
                        putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                        putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                    }

                    ContextCompat.startForegroundService(this, intent)
                    finish()
                }
            } ?: run {
                if (viewModel.skipFileDetails.value == true) finish()
            }
        }

    private var pendingSelectedUriDataList: List<UriData>? = null

    private val createZipFileLauncher: ActivityResultLauncher<CreateDocumentWithInitialUri.Input> =
        registerForActivityResult(CreateDocumentWithInitialUri()) { zipUri ->
            zipUri?.let { targetZipUri ->
                val uriDataList = pendingSelectedUriDataList ?: viewModel.uriDataList.value
                if (!uriDataList.isNullOrEmpty()) {
                    saveZippedFiles(targetZipUri, uriDataList)
                }
            } ?: run {
                if (viewModel.skipFileDetails.value == true) finish()
            }
        }

    private val openFolderLauncher: ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            treeUri?.let { folderUri ->
                val uriDataList = pendingSelectedUriDataList ?: viewModel.uriDataList.value
                if (!uriDataList.isNullOrEmpty()) {
                    saveMultipleFiles(folderUri, uriDataList)
                }
            } ?: run {
                if (viewModel.skipFileDetails.value == true) finish()
            }
        }

    private var sharedContent: CharSequence? = null
    private var sourceFileUri: Uri? = null
    private var sourceFileUriList: List<Uri>? = null

    private fun saveMultipleFiles(folderTreeUri: Uri, uriDataList: List<UriData>) {
        lifecycleScope.launch {
            val dir = DocumentFile.fromTreeUri(applicationContext, folderTreeUri) ?: return@launch
            val sourceUris = sourceFileUriList ?: return@launch

            sourceUris.forEachIndexed { index, sourceUri ->
                val data = uriDataList.getOrNull(index) ?: return@forEachIndexed
                val createdFile = dir.createFile(data.mimeType, data.displayName) ?: return@forEachIndexed

                val intent = Intent(this@DetailsActivity, SaveLocallyService::class.java).apply {
                    action = SaveLocallyService.ACTION_SAVE_FILE
                    putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceUri.toString())
                    putExtra(SaveLocallyService.EXTRA_TARGET_URI, createdFile.uri.toString())
                    putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                    putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                }
                ContextCompat.startForegroundService(this@DetailsActivity, intent)
            }
            finish()
        }
    }

    private fun saveMultipleFilesToDownloads(uriDataList: List<UriData>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        lifecycleScope.launch {
            val sourceUris = sourceFileUriList ?: return@launch

            sourceUris.forEachIndexed { index, sourceUri ->
                val data = uriDataList.getOrNull(index) ?: return@forEachIndexed
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, data.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, data.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val targetUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@forEachIndexed

                val intent = Intent(this@DetailsActivity, SaveLocallyService::class.java).apply {
                    action = SaveLocallyService.ACTION_SAVE_FILE
                    putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceUri.toString())
                    putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUri.toString())
                    putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                    putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                }
                ContextCompat.startForegroundService(this@DetailsActivity, intent)
            }
            finish()
        }
    }

    private fun saveZippedFiles(targetZipUri: Uri, uriDataList: List<UriData>) {
        lifecycleScope.launch {
            val sourceUris = uriDataList.map { it.uri }
            val fileNames = uriDataList.map { it.displayName }
            val defaultZipName = getString(R.string.save_as_zip_default_filename) + ".zip"

            val intent = Intent(this@DetailsActivity, SaveLocallyService::class.java).apply {
                action = SaveLocallyService.ACTION_SAVE_ZIPPED_FILES
                putStringArrayListExtra(SaveLocallyService.EXTRA_SOURCE_URIS, ArrayList(sourceUris))
                putStringArrayListExtra(SaveLocallyService.EXTRA_FILE_NAMES, ArrayList(fileNames))
                putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetZipUri.toString())
                putExtra(SaveLocallyService.EXTRA_FILE_NAME, defaultZipName)
            }
            ContextCompat.startForegroundService(this@DetailsActivity, intent)
            finish()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    this,
                    R.string.message_notification_permission_check_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()
        handleIntent(intent)

        setContent {
            val uriData by viewModel.uriData.collectAsState()
            val uriDataList by viewModel.uriDataList.collectAsState()
            val skipDetails by viewModel.skipFileDetails.collectAsState()
            val skipPicker by viewModel.skipFilePicker.collectAsState()
            val defaultLocation by viewModel.defaultSaveLocation.collectAsState()

            val launchFilePicker = { selectedItems: List<UriData>?, asZip: Boolean ->
                val data = uriData
                val dataList = selectedItems ?: uriDataList
                val initialUri = defaultLocation?.takeIf { it != AppConstants.MEDIASTORE_DOWNLOADS_URI }?.toUri()
                val zipDefaultName = getString(R.string.save_as_zip_default_filename) + ".zip"

                when {
                    sharedContent != null -> createFileLauncher.launch(
                        CreateDocumentWithInitialUri.Input(
                            "text.txt",
                            "text/plain",
                            initialUri
                        )
                    )

                    !dataList.isNullOrEmpty() && dataList.size > 1 -> {
                        pendingSelectedUriDataList = dataList
                        if (asZip) {
                            if (skipPicker == true && defaultLocation != null) {
                                val targetZipUri = if (defaultLocation == AppConstants.MEDIASTORE_DOWNLOADS_URI && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, zipDefaultName)
                                        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                    }
                                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                } else if (initialUri != null) {
                                    val dir = DocumentFile.fromTreeUri(applicationContext, initialUri)
                                    dir?.createFile("application/zip", zipDefaultName)?.uri
                                } else null

                                targetZipUri?.let { saveZippedFiles(it, dataList) }
                            } else {
                                createZipFileLauncher.launch(
                                    CreateDocumentWithInitialUri.Input(
                                        zipDefaultName,
                                        "application/zip",
                                        initialUri
                                    )
                                )
                            }
                        } else {
                            if (skipPicker == true && defaultLocation != null) {
                                if (defaultLocation == AppConstants.MEDIASTORE_DOWNLOADS_URI && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    saveMultipleFilesToDownloads(dataList)
                                } else if (initialUri != null) {
                                    saveMultipleFiles(initialUri, dataList)
                                }
                            } else {
                                openFolderLauncher.launch(initialUri)
                            }
                        }
                    }

                    data != null -> {
                        if (skipPicker == true && defaultLocation != null) {
                            lifecycleScope.launch {
                                val saveLocation = defaultLocation ?: return@launch
                                val targetUri = if (saveLocation == AppConstants.MEDIASTORE_DOWNLOADS_URI && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, data.displayName)
                                        put(MediaStore.MediaColumns.MIME_TYPE, data.mimeType)
                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                    }
                                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                } else {
                                    val dir = DocumentFile.fromTreeUri(
                                        applicationContext,
                                        saveLocation.toUri()
                                    )
                                    dir?.createFile(data.mimeType, data.displayName)?.uri
                                }

                                targetUri?.let { uri ->
                                    val intent = Intent(this@DetailsActivity, SaveLocallyService::class.java).apply {
                                        action = SaveLocallyService.ACTION_SAVE_FILE
                                        putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceFileUri.toString())
                                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, uri.toString())
                                        putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                                        putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                                    }

                                    ContextCompat.startForegroundService(this@DetailsActivity, intent)
                                    finish()
                                }
                            }
                        } else {
                            createFileLauncher.launch(
                                CreateDocumentWithInitialUri.Input(
                                    data.displayName,
                                    data.mimeType,
                                    initialUri
                                )
                            )
                        }
                    }
                }
            }

            // Wait for skipDetails to be loaded from preferences
            if (skipDetails != null) {
                if (skipDetails == true) {
                    LaunchedEffect(uriData, uriDataList, sharedContent, skipPicker, defaultLocation) {
                        if ((uriData != null || uriDataList != null || sharedContent != null) && skipPicker != null) {
                            launchFilePicker(null, false)
                        }
                    }
                    DetailsScreenSavingFileFeedback()
                } else {
                    if (!uriDataList.isNullOrEmpty() && uriDataList!!.size > 1) {
                        MultiDetailsScreen(
                            detailsViewModel = viewModel,
                            launchFilePicker = { selectedList, asZip -> launchFilePicker(selectedList, asZip) }
                        )
                    } else {
                        DetailsScreen(
                            detailsViewModel = viewModel,
                            launchFilePicker = { launchFilePicker(null, false) }
                        )
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val intent = intent ?: return
        val action = intent.action
        val type = intent.type

        val extraText = if (action == Intent.ACTION_SEND && type == "text/plain") {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        } else null

        if (extraText != null) {
            sharedContent = extraText
        } else {
            if (action == Intent.ACTION_SEND_MULTIPLE) {
                val streamUris = IntentCompat.getParcelableArrayListExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
                if (!streamUris.isNullOrEmpty()) {
                    sourceFileUriList = streamUris
                    sourceFileUri = streamUris.firstOrNull()
                    viewModel.loadMetadataList(streamUris.map { it.toString() })
                }
            } else {
                sourceFileUri = when (action) {
                    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )

                    Intent.ACTION_VIEW -> intent.data
                    else -> null
                }

                sourceFileUri?.let { uri ->
                    sourceFileUriList = listOf(uri)
                    viewModel.loadMetadata(uri.toString())
                }
            }
        }
    }
}
