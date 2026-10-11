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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.model.SampleMultiUriDataProvider
import com.mateusrodcosta.apps.share2storage.screens.shared.FileInfo
import com.mateusrodcosta.apps.share2storage.screens.shared.FilePreview
import com.mateusrodcosta.apps.share2storage.ui.theme.SaveLocallyTheme
import com.mateusrodcosta.apps.share2storage.utils.ui.shouldShowLandscape

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun MultiDetailsScreenPreview(@PreviewParameter(SampleMultiUriDataProvider ::class) uriDataList: List<UriData>?) {
    MultiDetailsScreen(uriDataList = uriDataList)
}

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun MultiDetailsScreenPreviewPtBr(@PreviewParameter(SampleMultiUriDataProvider ::class) uriDataList: List<UriData>?) {
    MultiDetailsScreen(uriDataList = uriDataList)
}

@Preview(apiLevel = 36, device = Devices.PIXEL_TABLET, showSystemUi = true, showBackground = true)
@Composable
fun MultiDetailsScreenPreviewLandscape(@PreviewParameter(SampleMultiUriDataProvider ::class) uriDataList: List<UriData>?) {
    MultiDetailsScreen(uriDataList = uriDataList)
}

@Preview(apiLevel = 36, device = Devices.PIXEL_TABLET, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun MultiDetailsScreenPreviewLandscapePtBr(@PreviewParameter(SampleMultiUriDataProvider ::class) uriDataList: List<UriData>?) {
    MultiDetailsScreen(uriDataList = uriDataList)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDetailsScreen(
    detailsViewModel: DetailsViewModel? = null,
    uriDataList: List<UriData>? = null,
    showFilePreview: Boolean = true,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    launchFilePicker: () -> Unit? = {},
) {
    val collectedUriDataList by detailsViewModel?.uriDataList?.collectAsState() ?: remember { mutableStateOf(uriDataList) }
    val collectedShowFilePreview by detailsViewModel?.showFilePreview?.collectAsState()
        ?: remember { mutableStateOf(showFilePreview) }

    val useLandscapeLayout = shouldShowLandscape(windowSizeClass)

    SaveLocallyTheme {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_details)) },
            )
        }, floatingActionButton = {
            if (collectedUriDataList != null) {
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
                if (collectedUriDataList != null) {
                    if (useLandscapeLayout) {
                        LazyRow(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(collectedUriDataList!!) { index, data ->
                                SingleFileDetailsLandscape(
                                    data,
                                    collectedShowFilePreview == true,
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    } else  {
                        LazyColumn {
                            itemsIndexed(collectedUriDataList!!) { index, data ->
                                SingleFileDetailsPortrait(data, collectedShowFilePreview == true)
                            }
                        }
                    }
                } else Text(
                    stringResource(R.string.no_file_found),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun SingleFileDetailsPortrait(uriData: UriData, showFilePreview: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = false, onCheckedChange = null)
        }
        FilePreview(uriData, showFilePreview = showFilePreview)
        Box {
            FileInfo(uriData, scrollable = false)
        }
    }
}

@Composable
fun SingleFileDetailsLandscape(
    uriData: UriData,
    showFilePreview: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = false, onCheckedChange = null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(2.0f)) {
                FilePreview(
                    uriData,
                    showFilePreview = showFilePreview,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Box(modifier = Modifier.weight(3.0f)) {
                FileInfo(uriData)
            }
        }
    }
}