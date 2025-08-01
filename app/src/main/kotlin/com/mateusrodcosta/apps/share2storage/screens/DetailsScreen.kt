/*
 *     Copyright (C) 2022 - 2025 Mateus Rodrigues Costa
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.model.SampleUriDataProvider
import com.mateusrodcosta.apps.share2storage.model.UriData
import com.mateusrodcosta.apps.share2storage.screens.shared.FileInfo
import com.mateusrodcosta.apps.share2storage.screens.shared.FilePreview
import com.mateusrodcosta.apps.share2storage.screens.shared.shouldShowLandscape
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme

@Preview(apiLevel = 34, showSystemUi = true, showBackground = true)
@Composable
fun DetailsScreenPreview(@PreviewParameter(SampleUriDataProvider::class) uriData: UriData?) {
    DetailsScreenContent(
        uriData = uriData,
        widthSizeClass = WindowWidthSizeClass.Compact,
        heightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(apiLevel = 34, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun DetailsScreenPreviewPtBr(@PreviewParameter(SampleUriDataProvider::class) uriData: UriData?) {
    DetailsScreenContent(
        uriData = uriData,
        widthSizeClass = WindowWidthSizeClass.Compact,
        heightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Composable
fun DetailsScreen(
    uriData: UriData?,
    windowSizeClass: WindowSizeClass,
    launchFilePicker: () -> Unit?,
) {
    DetailsScreenContent(
        uriData = uriData,
        widthSizeClass = windowSizeClass.widthSizeClass,
        heightSizeClass = windowSizeClass.heightSizeClass,
        launchFilePicker = launchFilePicker
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreenContent(
    uriData: UriData?,
    widthSizeClass: WindowWidthSizeClass,
    heightSizeClass: WindowHeightSizeClass,
    launchFilePicker: () -> Unit? = {},
) {
    AppTheme {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_details)) },
            )
        }, floatingActionButton = {
            if (uriData != null) {
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
                val showLandscape = shouldShowLandscape(widthSizeClass, heightSizeClass)
                if (uriData != null) {
                    if (showLandscape) FileDetailsLandscape(uriData)
                    else FileDetailsPortrait(uriData)
                } else Text(
                    stringResource(R.string.no_file_found),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun FileDetailsPortrait(uriData: UriData) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        FilePreview(uriData, modifier = Modifier.weight(1.0f))
        FileInfo(uriData, modifier = Modifier.verticalScroll(rememberScrollState()))
    }
}

@Composable
fun FileDetailsLandscape(uriData: UriData) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilePreview(
            uriData, modifier = Modifier.weight(1.0f)
        )
        FileInfo(uriData, modifier = Modifier.weight(1.0f))
    }
}


@Preview(apiLevel = 34, showSystemUi = true, showBackground = true)
@Composable
fun DetailsScreenSkippedPreview() {
    DetailsScreenSkippedContent()
}

@Preview(apiLevel = 34, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun DetailsScreenSkippedPreviewPtBr() {
    DetailsScreenSkippedContent()
}

@Composable
fun DetailsScreenSkipped() {
    DetailsScreenSkippedContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreenSkippedContent() {
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
