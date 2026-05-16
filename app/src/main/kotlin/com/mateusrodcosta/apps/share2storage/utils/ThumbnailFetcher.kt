/*
 *     Copyright (C) 2026 Mateus Rodrigues Costa
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

package com.mateusrodcosta.apps.share2storage.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.mateusrodcosta.apps.share2storage.model.MediaThumbnail

class ThumbnailFetcher(
    private val context: Context,
    private val data: MediaThumbnail
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(data.uri, data.size, null)
            } else {
                context.contentResolver.openFileDescriptor(data.uri, "r")?.use { fileDescriptor ->
                    val fd = fileDescriptor.fileDescriptor

                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFileDescriptor(fd, null, options)

                    options.inSampleSize = ThumbnailUtils.calculateInSampleSize(
                        options, data.size.width, data.size.height
                    )
                    options.inJustDecodeBounds = false

                    BitmapFactory.decodeFileDescriptor(fd, null, options)
                }
            }

            bitmap?.let {
                ImageFetchResult(
                    image = it.asImage(),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<MediaThumbnail> {
        override fun create(
            data: MediaThumbnail,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return ThumbnailFetcher(context, data)
        }
    }
}
