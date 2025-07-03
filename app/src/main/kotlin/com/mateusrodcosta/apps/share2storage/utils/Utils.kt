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

package com.mateusrodcosta.apps.share2storage.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import com.mateusrodcosta.apps.share2storage.model.UriData
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object Utils {
    const val CONTENT_ALPHA_DISABLED = 0.38f
}

fun getUriData(contentResolver: ContentResolver, uri: Uri, getPreview: Boolean): UriData? {
    val type = contentResolver.getType(uri) ?: "*/*"
    val displayName: String?
    val size: Long?

    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    val cursor = contentResolver.query(uri, projection, null, null, null)
    if (cursor == null) return null

    /*
     * Get the column indexes of the data in the Cursor,
     * move to the first row in the Cursor, get the data,
     * and display it.
     */
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
    cursor.moveToFirst()
    displayName = cursor.getString(nameIndex)
    size = cursor.getLong(sizeIndex)

    cursor.close()

    val bitmap = getUriThumbnail(getPreview, contentResolver, uri)
    return UriData(displayName, type, size, previewImage = bitmap)
}

private fun getUriThumbnail(
    getPreview: Boolean = false, contentResolver: ContentResolver, uri: Uri
): Bitmap? {
    var bitmap: Bitmap? = null
    if (getPreview) {
        // We need a max size in any case
        // I thought 1024x1024 might be too small so went with 2048x2048
        val maxThumbnailSize = Size(2048, 2048)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // The recommended approach for thumbnails using system APIs
                // While efficient it only supports Android 10+
                // This approach likely works with other formats aside than image, such as video
                bitmap = contentResolver.loadThumbnail(uri, maxThumbnailSize, null)
            } else {
                // For Android 9, Gemini suggested an approach where I get the image size and
                // manually scale it down
                // This works ony for images and adding video support might be a bit involved
                contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                    val fd = fileDescriptor.fileDescriptor
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFileDescriptor(fd, null, options)

                    val photoW = options.outWidth
                    val photoH = options.outHeight

                    var scaleFactor = 1
                    if (photoW > maxThumbnailSize.width || photoH > maxThumbnailSize.height) {
                        val halfPhotoW = photoW / 2
                        val halfPhotoH = photoH / 2

                        while (halfPhotoW / scaleFactor >= maxThumbnailSize.width && halfPhotoH / scaleFactor >= maxThumbnailSize.height) {
                            scaleFactor *= 2
                        }
                    }

                    options.inJustDecodeBounds = false
                    options.inSampleSize = scaleFactor

                    bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options)
                }
            }
        } catch (e: Exception) {
            Log.e("getUriThumbnail", e.message, e)
            bitmap = null
        }
    }

    return bitmap
}

private fun isVirtualFile(context: Context, contentResolver: ContentResolver, uri: Uri): Boolean {

    if (!DocumentsContract.isDocumentUri(context, uri)) {
        return false
    }
    val cursor: Cursor = contentResolver.query(
        uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null
    ) ?: return false
    var flags = 0
    if (cursor.moveToFirst()) {
        flags = cursor.getInt(0)
    }
    cursor.close()

    return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
}

@Throws(IOException::class)
private fun getInputStreamForVirtualFile(
    contentResolver: ContentResolver,
    uri: Uri,
): InputStream? {
    val filter = "*/*"
    val openableMimeTypes = contentResolver.getStreamTypes(uri, filter)
    if (openableMimeTypes.isNullOrEmpty()) {
        throw FileNotFoundException()
    }
    return contentResolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
        ?.createInputStream()
}

fun saveFileToFile(
    context: Context,
    contentResolver: ContentResolver,
    targetUri: Uri,
    sourceUri: Uri,
): Boolean {
    try {
        val inputStream = if (isVirtualFile(context, contentResolver, sourceUri)) {
            getInputStreamForVirtualFile(contentResolver, sourceUri)
        } else {
            contentResolver.openInputStream(sourceUri)
        }
        contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            inputStream?.use { inputStream ->
                saveToFile(inputStream, outputStream)
            } ?: return false
        } ?: return false
    } catch (e: Exception) {
        Log.e("saveFileToFile", e.message, e)
        return false
    }
    return true
}

fun saveTextToFile(
    contentResolver: ContentResolver,
    targetUri: Uri,
    content: CharSequence?,
): Boolean {
    content?.let {
        try {
            contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                it.toString().byteInputStream().use { inputStream ->
                    saveToFile(inputStream, outputStream)
                }
            } ?: return false
        } catch (e: Exception) {
            Log.e("saveTextToFile", e.message, e)
            return false
        }
        return true
    }
    return false
}

fun saveToFile(
    inputStream: InputStream,
    outputStream: OutputStream,
): Boolean {
    try {
        outputStream.use { outputStream ->
            inputStream.use { inputStream ->
                BufferedInputStream(inputStream).use { bis ->
                    BufferedOutputStream(outputStream).use { bos ->
                        bis.copyTo(bos)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("saveToFile", e.message, e)
        return false
    }
    return true
}
