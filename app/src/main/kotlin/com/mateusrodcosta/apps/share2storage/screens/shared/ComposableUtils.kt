/*
 *     Copyright (C) 2022 - 2024 Mateus Rodrigues Costa
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.graphics.vector.ImageVector
import com.mateusrodcosta.apps.share2storage.model.UriData

fun shouldShowLandscape(
    widthSizeClass: WindowWidthSizeClass,
    heightSizeClass: WindowHeightSizeClass,
): Boolean {
    val showLandscapePhone = heightSizeClass == WindowHeightSizeClass.Compact
    val showLandscapeTablet = widthSizeClass == WindowWidthSizeClass.Expanded
    return showLandscapePhone || showLandscapeTablet
}

fun getThumbnailFallbackInfo(uriData: UriData): ThumbnailFallbackInfo {
    val mimeType = uriData.mimeType
    val primaryType = mimeType?.substringBefore('/')

    return when (primaryType) {
        "image" -> ThumbnailFallbackInfo(
            Icons.Outlined.Image, "Image"
        )

        "audio" -> ThumbnailFallbackInfo(
            Icons.Outlined.AudioFile, "Audio"
        )

        "video" -> ThumbnailFallbackInfo(
            Icons.Outlined.VideoFile, "Video"
        )

        else -> ThumbnailFallbackInfo(
            Icons.Outlined.Description, "File"
        )
    }
}

data class ThumbnailFallbackInfo(val icon: ImageVector, val label: String)
