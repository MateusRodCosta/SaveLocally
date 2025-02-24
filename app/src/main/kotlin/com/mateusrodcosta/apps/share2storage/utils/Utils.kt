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
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.mateusrodcosta.apps.share2storage.model.UriData
import java.io.*

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

    var bitmap: Bitmap? = null
    if (getPreview) {
        try {
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                val fd = fileDescriptor.fileDescriptor
                bitmap = BitmapFactory.decodeFileDescriptor(fd)
            }
            fileDescriptor?.close()
        } catch (e: Exception) {
            Log.w("getUriData] bitmap", e.toString())
            bitmap = null
        }
    }

    return UriData(displayName, type, size, previewImage = bitmap)
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
        e.printStackTrace()
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
            e.printStackTrace()
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
        e.printStackTrace()
        return false
    }
    return true
}