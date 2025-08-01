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

package com.mateusrodcosta.apps.share2storage.screens.shared

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.model.UriData


@Composable
fun FileInfoLine(label: String, content: String, modifier: Modifier = Modifier, colors: ListItemColors ) {
    ListItem(modifier = modifier.clickable { }, headlineContent = {
        Text(label)
    }, supportingContent = {
        Text(content, softWrap = true)
    }, colors = colors)
}

@Composable
fun FileInfo(uriData: UriData, modifier: Modifier = Modifier, colors: ListItemColors = ListItemDefaults.colors() ) {
    Column(
        modifier = modifier, verticalArrangement = Arrangement.Center
    ) {
        FileInfoLine(
            label = stringResource(R.string.file_name), content = uriData.displayName, colors = colors
        )
        FileInfoLine(
            label = stringResource(R.string.file_type), content = uriData.mimeType ?: "*/*", colors = colors
        )
        FileInfoLine(
            label = stringResource(R.string.file_size),
            content = Formatter.formatFileSize(LocalContext.current, uriData.size), colors = colors
        )
    }
}


@Composable
fun FilePreview(uriData: UriData, modifier: Modifier = Modifier) {
    val fallbackInfo = getThumbnailFallbackInfo(uriData)

    Box(
        modifier = modifier.padding(16.dp)
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.align(Alignment.Center),
            model = uriData.uri,
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
                        imageVector = fallbackInfo.icon,
                        contentDescription = fallbackInfo.label,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}
