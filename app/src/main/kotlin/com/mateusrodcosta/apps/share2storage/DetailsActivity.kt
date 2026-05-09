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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.mateusrodcosta.apps.share2storage.data.repository.PreferencesRepositoryImpl
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreen
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreenSkipped
import com.mateusrodcosta.apps.share2storage.screens.SettingsViewModel
import com.mateusrodcosta.apps.share2storage.utils.CreateDocumentWithInitialUri
import com.mateusrodcosta.apps.share2storage.utils.getUriData
import com.mateusrodcosta.apps.share2storage.utils.saveFileToFile
import com.mateusrodcosta.apps.share2storage.utils.saveTextToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsActivity : ComponentActivity() {
    private var createFile: ActivityResultLauncher<String>? = null
    private var fileUri: Uri? = null
    private var uriData: UriData? = null
    private var content: CharSequence? = null

    private var defaultSaveLocation: Uri? = null
    private var shouldSkipFilePicker: Boolean = false
    private var skipFileDetails: Boolean = false
    private var shouldShowFilePreview: Boolean = true
    private var shouldFinishAfterSave: Boolean = false

    private val preferencesRepository: PreferencesRepository = PreferencesRepositoryImpl(this)
    private val settingsViewModel: SettingsViewModel = SettingsViewModel(preferencesRepository)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        getPreferences()
        handleIntent(intent)
        val launchFilePicker = {
            when {
                content != null -> createFile?.launch("text.txt")
                uriData != null -> {
                    if (shouldSkipFilePicker) {
                        lifecycleScope.launch {
                            val dir = defaultSaveLocation?.let {
                                DocumentFile.fromTreeUri(
                                    applicationContext, it
                                )
                            }
                            val file = dir?.createFile(uriData!!.mimeType, uriData!!.displayName)
                            file?.uri?.let { handleFileSave(it, fileUri!!) }
                        }
                    } else {
                        createFile?.launch(uriData!!.displayName)
                    }
                }
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            if (skipFileDetails) {
                LaunchedEffect(Unit) {
                    launchFilePicker()
                }
                DetailsScreenSkipped()
            } else {
                DetailsScreen(
                    uriData,
                    windowSizeClass,
                    launchFilePicker,
                )
            }
        }
    }

    private fun getPreferences() {

        defaultSaveLocation = settingsViewModel.defaultSaveLocation.value.let {
            try {
                it?.toUri()
            } catch (_: Exception) {
                null
            }
        }

        val skipFilePicker = settingsViewModel.skipFilePicker.value

        // Only skip file picker if both a default folder is set and "Skip File Picker is selected"
        this.shouldSkipFilePicker = defaultSaveLocation != null && skipFilePicker

        skipFileDetails = settingsViewModel.skipFileDetails.value

        val showFilePreview = settingsViewModel.showFilePreview.value

        this.shouldShowFilePreview = !skipFileDetails && showFilePreview
        this.shouldFinishAfterSave = skipFileDetails || shouldSkipFilePicker
    }

    private fun handleIntent(intent: Intent?) {
        val intent = intent ?: return

        val (action, type, data) = intent.let {
            Triple(it.action, it.type, it.data)
        }
        Log.d("getPreferences] handleIntent] intent", "action: $action, type: $type, data: $data")

        val content = when (action) {
            Intent.ACTION_SEND -> if (type == "text/plain") {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            } else null

            else -> null
        }
        Log.d("getPreferences] handleIntent] content", content.toString())

        val fileUri = when (action) {
            Intent.ACTION_SEND -> if (content == null) {
                IntentCompat.getParcelableExtra<Uri>(intent, Intent.EXTRA_STREAM, Uri::class.java)
            } else null

            Intent.ACTION_VIEW -> data
            else -> null
        }
        Log.d("getPreferences] handleIntent] fileUri", fileUri.toString())

        content?.let {
            createFile = registerForActivityResult(
                CreateDocumentWithInitialUri("text/plain", defaultSaveLocation)
            ) { uri ->
                if (uri == null) {
                    if (skipFileDetails) finish()
                } else {
                    lifecycleScope.launch {
                        handleTextSave(uri, content)
                    }
                }
            }
            this.content = it
            return
        }

        fileUri?.let {
            this.fileUri = fileUri
            val uriData =
                getUriData(contentResolver, fileUri, getPreview = shouldShowFilePreview) ?: return

            this.uriData = uriData

            createFile = registerForActivityResult(
                CreateDocumentWithInitialUri(uriData.mimeType, defaultSaveLocation)
            ) { uri ->
                if (uri == null) {
                    if (skipFileDetails) finish()
                } else {
                    lifecycleScope.launch {
                        handleFileSave(uri, fileUri)
                    }
                }
            }
        }
    }

    private suspend fun handleFileSave(uri: Uri, fileUri: Uri) {
        val isSuccess = withContext(Dispatchers.IO) {
            saveFileToFile(baseContext, baseContext.contentResolver, uri, fileUri)
        }

        withContext(Dispatchers.Main) {
            showResultToast(isSuccess)
            if (shouldFinishAfterSave) finish()
        }
    }

    private suspend fun handleTextSave(uri: Uri, content: CharSequence) {
        val isSuccess = withContext(Dispatchers.IO) {
            saveTextToFile(baseContext.contentResolver, uri, content)
        }

        withContext(Dispatchers.Main) {
            showResultToast(isSuccess)
            if (shouldFinishAfterSave) finish()
        }
    }

    private fun showResultToast(isSuccess: Boolean) {
        Toast.makeText(
            baseContext, if (isSuccess) {
                R.string.toast_saved_file_success
            } else {
                R.string.toast_saved_file_failure
            }, Toast.LENGTH_LONG
        ).show()
    }
}