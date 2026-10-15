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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mateusrodcosta.apps.share2storage.core.ui.theme.SaveLocallyTheme
import com.mateusrodcosta.apps.share2storage.core.ui.utils.shouldShowLandscape

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
    launchFilePicker: (List<UriData>, Boolean) -> Unit = { _, _ -> },
) {
    val collectedUriDataList by detailsViewModel?.uriDataList?.collectAsState() ?: remember { mutableStateOf(uriDataList) }
    val collectedShowFilePreview by detailsViewModel?.showFilePreview?.collectAsState()
        ?: remember { mutableStateOf(showFilePreview) }

    var isMenuExpanded by remember { mutableStateOf(false) }

    val selectedUris = remember(collectedUriDataList) {
        mutableStateMapOf<String, Boolean>().apply {
            collectedUriDataList?.forEach { put(it.uri, true) }
        }
    }

    val selectedItems = collectedUriDataList?.filter { selectedUris[it.uri] == true } ?: emptyList()
    val hasSelection = selectedItems.isNotEmpty()

    val totalCount = collectedUriDataList?.size ?: 0
    val allSelected = totalCount > 0 && selectedItems.size == totalCount

    val toggleSelectAll: () -> Unit = {
        val targetState = !allSelected
        collectedUriDataList?.forEach { data ->
            selectedUris[data.uri] = targetState
        }
    }

    val useLandscapeLayout = shouldShowLandscape(windowSizeClass)

    LaunchedEffect(selectedItems.size) {
        if (selectedItems.size <= 1) {
            isMenuExpanded = false
        }
    }

    BackHandler(enabled = isMenuExpanded && hasSelection && selectedItems.size > 1) {
        isMenuExpanded = false
    }

    SaveLocallyTheme {
        Scaffold(topBar = {
            TopAppBar(
                title = {
                    if (totalCount > 0) {
                        Text("${selectedItems.size} / $totalCount")
                    } else {
                        Text(stringResource(R.string.file_details))
                    }
                },
                actions = {
                    if (totalCount > 0) {
                        TextButton(onClick = toggleSelectAll) {
                            Text(
                                if (allSelected) stringResource(R.string.deselect_all)
                                else stringResource(R.string.select_all)
                            )
                        }
                    }
                }
            )
        }, floatingActionButton = {
            if (collectedUriDataList != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
                ) {
                    AnimatedVisibility(
                        visible = isMenuExpanded && hasSelection && selectedItems.size > 1,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + slideInVertically { it / 2 },
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FabMenuItem(
                                text = stringResource(R.string.save_files_separately),
                                icon = Icons.Rounded.Download,
                                onClick = {
                                    isMenuExpanded = false
                                    launchFilePicker(selectedItems, false)
                                }
                            )
                            FabMenuItem(
                                text = stringResource(R.string.save_as_zip),
                                icon = Icons.Rounded.Archive,
                                onClick = {
                                    isMenuExpanded = false
                                    launchFilePicker(selectedItems, true)
                                }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            if (hasSelection) {
                                if (selectedItems.size > 1) {
                                    isMenuExpanded = !isMenuExpanded
                                } else {
                                    launchFilePicker(selectedItems, false)
                                }
                            }
                        },
                        content = {
                            Icon(
                                imageVector = if (isMenuExpanded && selectedItems.size > 1) Icons.Rounded.Close else Icons.Rounded.Download,
                                contentDescription = stringResource(R.string.save_button)
                            )
                        },
                        containerColor = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hasSelection) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                            itemsIndexed(collectedUriDataList!!) { _, data ->
                                SingleFileDetailsLandscape(
                                    uriData = data,
                                    showFilePreview = collectedShowFilePreview == true,
                                    isChecked = selectedUris[data.uri] == true,
                                    onToggleSelection = {
                                        selectedUris[data.uri] = selectedUris[data.uri] != true
                                    },
                                    onSaveIndividualFile = {
                                        launchFilePicker(listOf(data), false)
                                    },
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    } else {
                        LazyColumn {
                            itemsIndexed(collectedUriDataList!!) { _, data ->
                                SingleFileDetailsPortrait(
                                    uriData = data,
                                    showFilePreview = collectedShowFilePreview == true,
                                    isChecked = selectedUris[data.uri] == true,
                                    onToggleSelection = {
                                        selectedUris[data.uri] = selectedUris[data.uri] != true
                                    },
                                    onSaveIndividualFile = {
                                        launchFilePicker(listOf(data), false)
                                    }
                                )
                            }
                        }
                    }

                    if (isMenuExpanded && hasSelection && selectedItems.size > 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isMenuExpanded = false }
                        )
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
fun SingleFileHeader(
    isChecked: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveIndividualFile: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { onToggleSelection() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                modifier = Modifier.padding(horizontal = 8.dp),
                checked = isChecked,
                onCheckedChange = { onToggleSelection() }
            )
        }
        IconButton(onClick = onSaveIndividualFile) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = stringResource(R.string.save_button)
            )
        }
    }
}

@Composable
fun SingleFileDetailsPortrait(
    uriData: UriData,
    showFilePreview: Boolean,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onToggleSelection: () -> Unit,
    onSaveIndividualFile: () -> Unit = {}
) {
    Card(
        modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleFileHeader(
                isChecked = isChecked,
                onToggleSelection = onToggleSelection,
                onSaveIndividualFile = onSaveIndividualFile
            )
            FilePreview(uriData, showFilePreview = showFilePreview)
            Box {
                FileInfo(uriData, scrollable = false)
            }
        }
    }
}

@Composable
fun SingleFileDetailsLandscape(
    uriData: UriData,
    showFilePreview: Boolean,
    isChecked: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveIndividualFile: () -> Unit = {},
) {
    Card(
        modifier.padding(horizontal = 64.dp, vertical = 32.dp)
    ) {
        Column {
            SingleFileHeader(
                isChecked = isChecked,
                onToggleSelection = onToggleSelection,
                onSaveIndividualFile = onSaveIndividualFile
            )
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
}

@Composable
fun FabMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        }
    }
}