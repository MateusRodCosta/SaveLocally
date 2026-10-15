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
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import com.mateusrodcosta.apps.share2storage.core.AppConstants
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.features.details.screens.DetailsScreen
import com.mateusrodcosta.apps.share2storage.features.details.screens.DetailsScreenSavingFileFeedback
import com.mateusrodcosta.apps.share2storage.features.details.screens.DetailsScreenStrings
import com.mateusrodcosta.apps.share2storage.features.details.screens.DetailsViewModel
import com.mateusrodcosta.apps.share2storage.features.details.screens.MultiDetailsScreen
import com.mateusrodcosta.apps.share2storage.features.details.utils.result.CreateDocumentWithInitialUri
import com.mateusrodcosta.apps.share2storage.service.SaveLocallyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailsActivity : ComponentActivity() {

    private val viewModel: DetailsViewModel by viewModel()

    private var sharedContent: CharSequence? = null
    private var sourceFileUri: Uri? = null
    private var sourceFileUriList: List<Uri>? = null

    private fun Intent.grantUriPermissions(sourceUris: List<Uri>?, targetUri: Uri?): Intent {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (targetUri != null) {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val items = mutableListOf<ClipData.Item>()
        sourceUris?.forEach { uri ->
            items.add(ClipData.Item(uri))
        }
        if (targetUri != null) {
            items.add(ClipData.Item(targetUri))
        }

        if (items.isNotEmpty()) {
            val clipData = ClipData("uris", arrayOf("text/uri-list"), items.first())
            for (i in 1 until items.size) {
                clipData.addItem(items[i])
            }
            this.clipData = clipData
        }
        return this
    }

    private fun startSaveService(
        action: String,
        targetUri: Uri,
        sourceUris: List<Uri>? = sourceFileUriList ?: sourceFileUri?.let { listOf(it) },
        setup: Intent.() -> Unit = {}
    ) {
        val intent = Intent(this, SaveLocallyService::class.java).apply {
            this.action = action
            grantUriPermissions(sourceUris, targetUri)
            setup()
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private val createFileLauncher: ActivityResultLauncher<CreateDocumentWithInitialUri.Input> =
        registerForActivityResult(CreateDocumentWithInitialUri()) { uri ->
            uri?.let { targetUri ->
                if (sharedContent != null) {
                    startSaveService(SaveLocallyService.ACTION_SAVE_TEXT, targetUri) {
                        putExtra(SaveLocallyService.EXTRA_TEXT, sharedContent.toString())
                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUri.toString())
                    }
                } else {
                    val data = checkNotNull(viewModel.uriData.value) { "UriData metadata must be loaded before saving" }
                    startSaveService(SaveLocallyService.ACTION_SAVE_FILE, targetUri) {
                        putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceFileUri.toString())
                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUri.toString())
                        putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                        putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                    }
                }
                finish()
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
                    saveMultipleFiles(folderUri.toString(), uriDataList)
                }
            } ?: run {
                if (viewModel.skipFileDetails.value == true) finish()
            }
        }

    private fun saveMultipleFiles(locationUriString: String, uriDataList: List<UriData>) {
        lifecycleScope.launch(Dispatchers.IO) {
            uriDataList.forEach { data ->
                val sourceUri = data.uri.toUri()
                val targetUriString = viewModel.createTargetUri(
                    locationUriString = locationUriString,
                    displayName = data.displayName,
                    mimeType = data.mimeType
                ).getOrNull() ?: return@forEach

                val targetUri = targetUriString.toUri()
                startSaveService(
                    action = SaveLocallyService.ACTION_SAVE_FILE,
                    targetUri = targetUri,
                    sourceUris = listOf(sourceUri)
                ) {
                    putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceUri.toString())
                    putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUriString)
                    putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                    putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                }
            }
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun saveZippedFiles(targetZipUri: Uri, uriDataList: List<UriData>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sourceUris = uriDataList.map { it.uri }
            val fileNames = uriDataList.map { it.displayName }
            val defaultZipName = getString(R.string.save_as_zip_default_filename) + ".zip"

            startSaveService(
                action = SaveLocallyService.ACTION_SAVE_ZIPPED_FILES,
                targetUri = targetZipUri,
                sourceUris = sourceUris.map { it.toUri() }
            ) {
                putStringArrayListExtra(SaveLocallyService.EXTRA_SOURCE_URIS, ArrayList(sourceUris))
                putStringArrayListExtra(SaveLocallyService.EXTRA_FILE_NAMES, ArrayList(fileNames))
                putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetZipUri.toString())
                putExtra(SaveLocallyService.EXTRA_FILE_NAME, defaultZipName)
            }
            withContext(Dispatchers.Main) { finish() }
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
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val targetZipUriString = viewModel.createTargetUri(
                                        locationUriString = defaultLocation!!,
                                        displayName = zipDefaultName,
                                        mimeType = "application/zip"
                                    ).getOrNull()

                                    if (targetZipUriString != null) {
                                        saveZippedFiles(targetZipUriString.toUri(), dataList)
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            if (viewModel.skipFileDetails.value == true) finish()
                                        }
                                    }
                                }
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
                                saveMultipleFiles(defaultLocation!!, dataList)
                            } else {
                                openFolderLauncher.launch(initialUri)
                            }
                        }
                    }

                    data != null -> {
                        if (skipPicker == true && defaultLocation != null) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val saveLocation = defaultLocation ?: return@launch
                                val targetUriString = viewModel.createTargetUri(
                                    locationUriString = saveLocation,
                                    displayName = data.displayName,
                                    mimeType = data.mimeType
                                ).getOrNull()

                                if (targetUriString != null) {
                                    val targetUri = targetUriString.toUri()
                                    startSaveService(SaveLocallyService.ACTION_SAVE_FILE, targetUri) {
                                        putExtra(SaveLocallyService.EXTRA_SOURCE_URI, sourceFileUri.toString())
                                        putExtra(SaveLocallyService.EXTRA_TARGET_URI, targetUriString)
                                        putExtra(SaveLocallyService.EXTRA_FILE_NAME, data.displayName)
                                        putExtra(SaveLocallyService.EXTRA_FILE_SIZE, data.size)
                                    }
                                    withContext(Dispatchers.Main) { finish() }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        if (viewModel.skipFileDetails.value == true) finish()
                                    }
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

            val detailsStrings = DetailsScreenStrings(
                fileDetails = getString(R.string.file_details),
                saveButton = getString(R.string.save_button),
                noFileFound = getString(R.string.no_file_found),
                savingFile = getString(R.string.saving_file),
                selectAll = getString(R.string.select_all),
                deselectAll = getString(R.string.deselect_all),
                saveFilesSeparately = getString(R.string.save_files_separately),
                saveAsZip = getString(R.string.save_as_zip),
                fileName = getString(R.string.file_name),
                fileType = getString(R.string.file_type),
                fileSize = getString(R.string.file_size),
                appName = getString(R.string.app_name)
            )

            // Wait for skipDetails to be loaded from preferences
            if (skipDetails != null) {
                if (skipDetails == true) {
                    LaunchedEffect(uriData, uriDataList, sharedContent, skipPicker, defaultLocation) {
                        if ((uriData != null || uriDataList != null || sharedContent != null) && skipPicker != null) {
                            launchFilePicker(null, false)
                        }
                    }
                    DetailsScreenSavingFileFeedback(strings = detailsStrings)
                } else {
                    if (!uriDataList.isNullOrEmpty() && uriDataList!!.size > 1) {
                        MultiDetailsScreen(
                            detailsViewModel = viewModel,
                            launchFilePicker = { selectedList, asZip -> launchFilePicker(selectedList, asZip) },
                            strings = detailsStrings
                        )
                    } else {
                        DetailsScreen(
                            detailsViewModel = viewModel,
                            launchFilePicker = { launchFilePicker(null, false) },
                            strings = detailsStrings
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
