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

package com.mateusrodcosta.apps.share2storage.features.details.screens.shared

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mateusrodcosta.apps.share2storage.core.ui.model.MediaThumbnail
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.core.resources.R as RResources

@Composable
fun FileInfoLine(label: String, content: String) {
    ListItem(
        headlineContent = {
            Text(label)
        },
        supportingContent = {
            Text(content, softWrap = true)
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            leadingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun FileInfo(
    uriData: UriData,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val fileNameLabel = stringResource(RResources.string.file_name)
    val fileTypeLabel = stringResource(RResources.string.file_type)
    val fileSizeLabel = stringResource(RResources.string.file_size)

    val columnModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.Center
    ) {
        FileInfoLine(
            label = fileNameLabel,
            content = uriData.displayName
        )
        FileInfoLine(
            label = fileTypeLabel,
            content = uriData.mimeType
        )
        FileInfoLine(
            label = fileSizeLabel,
            content = Formatter.formatFileSize(LocalContext.current, uriData.size)
        )
    }
}

@Composable
fun FilePreview(
    uriData: UriData,
    modifier: Modifier = Modifier,
    showFilePreview: Boolean = true
) {
    val appName = stringResource(RResources.string.app_name)
    val mimeType = uriData.mimeType
    val primaryType = mimeType.substringBefore('/')
    val fallbackFileIcon = when (primaryType) {
        "image" -> Icons.Outlined.Image
        "audio" -> Icons.Outlined.AudioFile
        "video" -> Icons.Outlined.VideoFile
        else -> Icons.Outlined.Description
    }

    Box(
        modifier = modifier
            .padding(16.dp)
    ) {
        if (showFilePreview) {
            SubcomposeAsyncImage(
                modifier = Modifier.align(Alignment.Center),
                model = MediaThumbnail(uriData.uri.toUri(), mimeType = uriData.mimeType),
                contentDescription = appName,
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
                            contentDescription = appName,
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
                contentDescription = appName,
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
