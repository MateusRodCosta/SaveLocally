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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreen
import com.mateusrodcosta.apps.share2storage.screens.DetailsScreenSkipped
import com.mateusrodcosta.apps.share2storage.screens.DetailsViewModel
import com.mateusrodcosta.apps.share2storage.utils.CreateDocumentWithInitialUri
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailsActivity : ComponentActivity() {

    private val viewModel: DetailsViewModel by viewModel()

    private var createFileLauncher: ActivityResultLauncher<String>? = null

    private var sharedContent: CharSequence? = null
    private var sourceFileUri: Uri? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uriData by viewModel.uriData.collectAsState()
            val saveResult by viewModel.saveResult.collectAsState()
            val skipDetails by viewModel.skipFileDetails.collectAsState()
            val skipPicker by viewModel.skipFilePicker.collectAsState()
            val defaultLocation by viewModel.defaultSaveLocation.collectAsState()

            val launchFilePicker = {
                val data = uriData
                when {
                    sharedContent != null -> createFileLauncher?.launch("text.txt")
                    data != null -> {
                        if (skipPicker && defaultLocation != null) {
                            lifecycleScope.launch {
                                val dir = DocumentFile.fromTreeUri(applicationContext, defaultLocation!!.toUri())
                                val file = dir?.createFile(data.mimeType, data.displayName)
                                file?.uri?.let { viewModel.saveFile(sourceFileUri.toString(), it.toString()) }
                            }
                        } else {
                            createFileLauncher?.launch(data.displayName)
                        }
                    }
                }
            }

            LaunchedEffect(saveResult) {
                saveResult?.let { result ->
                    val message = if (result.isSuccess) R.string.toast_saved_file_success else R.string.toast_saved_file_failure
                    Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
                    
                    if (skipDetails || (skipPicker && defaultLocation != null)) {
                        finish()
                    }
                    viewModel.resetSaveResult()
                }
            }

            if (skipDetails) {
                LaunchedEffect(uriData, sharedContent) {
                    if (uriData != null || sharedContent != null) {
                        launchFilePicker()
                    }
                }
                DetailsScreenSkipped()
            } else {
                DetailsScreen(
                    uriData = uriData,
                    windowSizeClass = windowSizeClass,
                    launchFilePicker = { launchFilePicker() }
                )
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val intent = intent ?: return
        val action = intent.action
        val type = intent.type

        if (action == Intent.ACTION_SEND && type == "text/plain") {
            sharedContent = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            registerFilePicker("text/plain")
        } else {
            sourceFileUri = when (action) {
                Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }
            
            sourceFileUri?.let { uri ->
                viewModel.loadMetadata(uri.toString())
                registerFilePicker(type ?: "*/*")
            }
        }
    }

    private fun registerFilePicker(mimeType: String) {
        val defaultLocation = viewModel.defaultSaveLocation.value?.toUri()
        createFileLauncher = registerForActivityResult(
            CreateDocumentWithInitialUri(mimeType, defaultLocation)
        ) { uri ->
            uri?.let { targetUri ->
                if (sharedContent != null) {
                    viewModel.saveText(sharedContent.toString(), targetUri.toString())
                } else {
                    sourceFileUri?.let { sourceUri ->
                        viewModel.saveFile(sourceUri.toString(), targetUri.toString())
                    }
                }
            } ?: run {
                if (viewModel.skipFileDetails.value) finish()
            }
        }
    }
}
