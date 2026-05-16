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

package com.mateusrodcosta.apps.share2storage.screens

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.model.MediaThumbnail
import com.mateusrodcosta.apps.share2storage.model.SampleUriDataProvider
import com.mateusrodcosta.apps.share2storage.screens.shared.shouldShowLandscape
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun DetailsScreenPreview(@PreviewParameter(SampleUriDataProvider::class) uriData: UriData?) {
    DetailsScreen(uriData = uriData)
}

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun DetailsScreenPreviewPtBr(@PreviewParameter(SampleUriDataProvider::class) uriData: UriData?) {
    DetailsScreen(uriData = uriData)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    detailsViewModel: DetailsViewModel? = null,
    uriData: UriData? = null,
    showFilePreview: Boolean = true,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass,
    launchFilePicker: () -> Unit? = {},
) {
    val collectedUriData by if (detailsViewModel != null) {
        detailsViewModel.uriData.collectAsState()
    } else {
        remember { mutableStateOf(uriData) }
    }
    val collectedShowFilePreview by if (detailsViewModel != null) {
        detailsViewModel.showFilePreview.collectAsState()
    } else {
        remember { mutableStateOf(showFilePreview) }
    }

    val useLandscapeLayout = shouldShowLandscape(windowSizeClass)

    AppTheme {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_details)) },
            )
        }, floatingActionButton = {
            if (collectedUriData != null) {
                FloatingActionButton(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Horizontal
                        )
                    ),
                    onClick = { launchFilePicker() },
                    content = {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.save_button)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }) { paddingValues ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (collectedUriData != null) {
                    if (useLandscapeLayout) FileDetailsLandscape(collectedUriData!!, collectedShowFilePreview)
                    else FileDetailsPortrait(collectedUriData!!, collectedShowFilePreview)
                } else Text(
                    stringResource(R.string.no_file_found),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun FileDetailsPortrait(uriData: UriData, showFilePreview: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1.0f)
        ) {
            FilePreview(uriData, showFilePreview)
        }
        Box {
            FileInfo(uriData)
        }
    }
}

@Composable
fun FileDetailsLandscape(uriData: UriData, showFilePreview: Boolean) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1.0f)) {
            FilePreview(uriData, showFilePreview)
        }
        Box(modifier = Modifier.weight(1.0f)) {
            FileInfo(uriData)
        }
    }
}

@Composable
fun FileInfo(uriData: UriData) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        FileInfoLine(
            label = stringResource(R.string.file_name),
            content = uriData.displayName
        )
        FileInfoLine(
            label = stringResource(R.string.file_type),
            content = uriData.mimeType
        )
        FileInfoLine(
            label = stringResource(R.string.file_size),
            content = Formatter.formatFileSize(LocalContext.current, uriData.size)
        )
    }
}

@Composable
fun FileInfoLine(label: String, content: String) {
    ListItem(modifier = Modifier.clickable { }, headlineContent = {
        Text(label)
    }, supportingContent = {
        Text(content, softWrap = true)
    })
}

@Composable
fun FilePreview(uriData: UriData, showFilePreview: Boolean = true) {
    val mimeType = uriData.mimeType
    val primaryType = mimeType.substringBefore('/')
    val fallbackFileIcon = when (primaryType) {
        "image" -> Icons.Outlined.Image
        "audio" -> Icons.Outlined.AudioFile
        "video" -> Icons.Outlined.VideoFile
        else -> Icons.Outlined.Description
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (showFilePreview) {
            SubcomposeAsyncImage(
                modifier = Modifier.align(Alignment.Center),
                model = MediaThumbnail(uriData.uri.toUri()),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
            ) {
                val state by painter.state.collectAsState()

                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        CircularProgressIndicator()
                    }
                    // Display fallback icon if can't create thumbnail
                    is AsyncImagePainter.State.Error -> {
                        Icon(
                            modifier = Modifier
                                .size(128.dp)
                                .align(Alignment.Center),
                            imageVector = fallbackFileIcon,
                            contentDescription = stringResource(R.string.app_name),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        } else {
            Icon(
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.Center),
                imageVector = fallbackFileIcon,
                contentDescription = stringResource(R.string.app_name),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun DetailsScreenSkippedPreview() {
    DetailsScreenSkipped()
}

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun DetailsScreenSkippedPreviewPtBr() {
    DetailsScreenSkipped()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreenSkipped() {
    AppTheme {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_details)) },
            )
        }) { paddingValues ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    stringResource(R.string.saving_file),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
