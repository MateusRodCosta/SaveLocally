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

    private val createFileLauncher: ActivityResultLauncher<CreateDocumentWithInitialUri.Input> =
        registerForActivityResult(CreateDocumentWithInitialUri()) { uri ->
            uri?.let { targetUri ->
                if (sharedContent != null) {
                    viewModel.saveText(sharedContent.toString(), targetUri.toString())
                } else {
                    sourceFileUri?.let { sourceUri ->
                        viewModel.saveFile(sourceUri.toString(), targetUri.toString())
                    }
                }
            } ?: run {
                if (viewModel.skipFileDetails.value == true) finish()
            }
        }

    private var sharedContent: CharSequence? = null
    private var sourceFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val uriData by viewModel.uriData.collectAsState()
            val saveResult by viewModel.saveResult.collectAsState()
            val skipDetails by viewModel.skipFileDetails.collectAsState()
            val skipPicker by viewModel.skipFilePicker.collectAsState()
            val defaultLocation by viewModel.defaultSaveLocation.collectAsState()

            val launchFilePicker = {
                val data = uriData
                val initialUri = defaultLocation?.toUri()
                when {
                    sharedContent != null -> createFileLauncher.launch(
                        CreateDocumentWithInitialUri.Input(
                            "text.txt",
                            "text/plain",
                            initialUri
                        )
                    )

                    data != null -> {
                        if (skipPicker == true && initialUri != null) {
                            lifecycleScope.launch {
                                val dir = DocumentFile.fromTreeUri(
                                    applicationContext,
                                    initialUri
                                )
                                val file = dir?.createFile(data.mimeType, data.displayName)
                                file?.uri?.let {
                                    viewModel.saveFile(
                                        sourceFileUri.toString(),
                                        it.toString()
                                    )
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

            LaunchedEffect(saveResult) {
                saveResult?.let { result ->
                    val message =
                        if (result.isSuccess) R.string.toast_saved_file_success else R.string.toast_saved_file_failure
                    Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()

                    if (skipDetails == true || (skipPicker == true && defaultLocation != null)) {
                        finish()
                    }
                    viewModel.resetSaveResult()
                }
            }

            // Wait for skipDetails to be loaded from preferences
            if (skipDetails != null) {
                if (skipDetails == true) {
                    LaunchedEffect(uriData, sharedContent, skipPicker, defaultLocation) {
                        if ((uriData != null || sharedContent != null) && skipPicker != null) {
                            launchFilePicker()
                        }
                    }
                    DetailsScreenSkipped()
                } else {
                    DetailsScreen(
                        detailsViewModel = viewModel,
                        launchFilePicker = { launchFilePicker() }
                    )
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
                viewModel.loadMetadata(uri.toString())
            }
        }
    }
}
