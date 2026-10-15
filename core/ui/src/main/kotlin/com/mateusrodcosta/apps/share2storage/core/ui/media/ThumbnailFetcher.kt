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

package com.mateusrodcosta.apps.share2storage.core.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.mateusrodcosta.apps.share2storage.core.ui.model.MediaThumbnail

class ThumbnailFetcher(
    private val context: Context,
    private val data: MediaThumbnail
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return try {
            val resolvedMimeType = runCatching {
                context.contentResolver.getType(data.uri)
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: data.mimeType

            val primaryType = resolvedMimeType.substringBefore('/')

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    context.contentResolver.loadThumbnail(data.uri, data.size, null)
                }.getOrNull() ?: fetchFallbackBitmap(resolvedMimeType, primaryType)
            } else {
                fetchFallbackBitmap(resolvedMimeType, primaryType)
            }

            bitmap?.let {
                ImageFetchResult(
                    image = it.asImage(),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFallbackBitmap(mimeType: String, primaryType: String): Bitmap? {
        return when {
            primaryType == "video" -> {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        context.contentResolver.openFileDescriptor(data.uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            retriever.getScaledFrameAtTime(
                                -1L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                data.size.width,
                                data.size.height
                            ) ?: retriever.frameAtTime
                        }
                    } finally {
                        runCatching { retriever.release() }
                    }
                }.getOrNull()
            }

            primaryType == "audio" -> {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        context.contentResolver.openFileDescriptor(data.uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            val picture = retriever.embeddedPicture
                            picture?.let { bytes ->
                                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
                                val sampleSize = ThumbnailUtils.calculateInSampleSize(
                                    boundsOptions, data.size.width, data.size.height
                                )
                                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                            }
                        }
                    } finally {
                        runCatching { retriever.release() }
                    }
                }.getOrNull()
            }

            mimeType == "application/pdf" -> {
                runCatching {
                    context.contentResolver.openFileDescriptor(data.uri, "r")?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            if (renderer.pageCount > 0) {
                                renderer.openPage(0).use { page ->
                                    val targetWidth = if (page.width > 0) page.width.coerceAtMost(data.size.width) else data.size.width
                                    val targetHeight = if (page.height > 0) page.height.coerceAtMost(data.size.height) else data.size.height
                                    createBitmap(targetWidth, targetHeight).also { bitmap ->
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                            } else null
                        }
                    }
                }.getOrNull()
            }

            else -> {
                runCatching {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(data.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }

                    val sampleSize = ThumbnailUtils.calculateInSampleSize(
                        boundsOptions, data.size.width, data.size.height
                    )

                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val decodedBitmap = context.contentResolver.openInputStream(data.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }

                    decodedBitmap?.let { bitmap ->
                        val rotation = context.contentResolver.openInputStream(data.uri)?.use { stream ->
                            val exif = ExifInterface(stream)
                            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                                else -> 0
                            }
                        } ?: 0

                        if (rotation != 0) {
                            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap
                        }
                    }
                }.getOrNull()
            }
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
