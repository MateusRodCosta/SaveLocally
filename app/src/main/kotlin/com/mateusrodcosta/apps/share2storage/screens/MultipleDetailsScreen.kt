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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.model.SampleUriDataListProvider
import com.mateusrodcosta.apps.share2storage.model.UriData
import com.mateusrodcosta.apps.share2storage.screens.shared.AppBasicDivider
import com.mateusrodcosta.apps.share2storage.screens.shared.FileInfo
import com.mateusrodcosta.apps.share2storage.screens.shared.FilePreview
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme

@Preview(apiLevel = 34, showSystemUi = true, showBackground = true)
@Composable
fun MultipleDetailsScreenPreview(@PreviewParameter(SampleUriDataListProvider::class) uriDataList: List<UriData>?) {
    MultipleDetailsScreenContent(
        uriDataList = uriDataList,
        widthSizeClass = WindowWidthSizeClass.Compact,
        heightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Preview(apiLevel = 34, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun MultipleDetailsScreenPreviewPtBr(@PreviewParameter(SampleUriDataListProvider::class) uriDataList: List<UriData>?) {
    MultipleDetailsScreenContent(
        uriDataList = uriDataList,
        widthSizeClass = WindowWidthSizeClass.Compact,
        heightSizeClass = WindowHeightSizeClass.Medium,
    )
}

@Composable
fun MultipleDetailsScreen(
    uriDataList: List<UriData>?,
    windowSizeClass: WindowSizeClass,
) {
    MultipleDetailsScreenContent(
        uriDataList = uriDataList,
        widthSizeClass = windowSizeClass.widthSizeClass,
        heightSizeClass = windowSizeClass.heightSizeClass,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleDetailsScreenContent(
    uriDataList: List<UriData>?,
    widthSizeClass: WindowWidthSizeClass,
    heightSizeClass: WindowHeightSizeClass,
) {
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.file_details)) },
                )
            },
        ) { paddingValues ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                //shouldShowLandscape(widthSizeClass, heightSizeClass)
                if (uriDataList != null) {
                    //if (showLandscape) MultipleFileDetailsLandscape(uriDataList)
                    //else
                    MultipleFileDetailsPortrait(uriDataList)
                } else Text(
                    stringResource(R.string.no_file_found),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

    }
}

@Composable
fun CollapsibleFileDetailsHeader(uriData: UriData, isCollapsed: Boolean, onToggle: () -> Unit) {
    val dropdownIcon = if (isCollapsed) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = dropdownIcon, contentDescription = null
                )
            }
            Text(uriData.displayName, modifier = Modifier.weight(1.0f))
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Save, contentDescription = null
                )
            }
        }
    }
}

@Composable
fun CollapsibleFileDetailsContent(
    uriData: UriData, colors: CardColors = CardDefaults.cardColors()
) {
    val defaultColors = ListItemDefaults.colors()
    val convertedColors = ListItemColors(
        containerColor = colors.containerColor,
        headlineColor = defaultColors.headlineColor,
        leadingIconColor = defaultColors.leadingIconColor,
        overlineColor = defaultColors.overlineColor,
        supportingTextColor = defaultColors.supportingTextColor,
        trailingIconColor = defaultColors.trailingIconColor,
        disabledHeadlineColor = defaultColors.disabledHeadlineColor,
        disabledLeadingIconColor = defaultColors.disabledLeadingIconColor,
        disabledTrailingIconColor = defaultColors.disabledTrailingIconColor
    )

    FilePreview(uriData, modifier = Modifier.fillMaxWidth())
    FileInfo(uriData, colors = convertedColors)
}

@Composable
fun CollapsibleFileDetail(uriData: UriData) {
    var isCollapsed by remember { mutableStateOf(false) }
    Card {
        Column {
            CollapsibleFileDetailsHeader(
                uriData, isCollapsed, onToggle = { isCollapsed = !isCollapsed })
            AnimatedVisibility(!isCollapsed) {
                Column {
                    AppBasicDivider()
                    CollapsibleFileDetailsContent(uriData, colors = CardDefaults.cardColors())
                }

            }
        }
    }
}

@Composable
fun MultipleFileDetailsPortrait(uriDataList: List<UriData>) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(uriDataList) { uriData ->
            CollapsibleFileDetail(uriData)
        }
    }
}

/*
@Composable
fun MultipleFileDetailsLandscape(uriDataList: List<UriData>) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(uriDataList) { uriData ->
            CollapsibleFileDetail(uriData)
        }
    }
}

 */
