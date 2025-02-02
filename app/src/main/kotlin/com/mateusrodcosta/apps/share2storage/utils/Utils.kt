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
    const val BUFFER_SIZE: Int = 1024
    const val CONTENT_ALPHA_DISABLED = 0.38f
}

fun getUriData(contentResolver: ContentResolver, uri: Uri, getPreview: Boolean): UriData? {
    val type = contentResolver.getType(uri) ?: "*/*"
    val displayName: String?
    val size: Long?

    val cursor = contentResolver.query(uri, null, null, null, null)
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

private fun isVirtualFile(context: Context, uri: Uri): Boolean {

    if (!DocumentsContract.isDocumentUri(context, uri)) {
        return false
    }
    val cursor: Cursor = context.contentResolver.query(
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
    context: Context,
    uri: Uri,
): InputStream? {
    val resolver = context.contentResolver
    val filter = "*/*"
    val openableMimeTypes = resolver.getStreamTypes(uri, filter)
    if (openableMimeTypes.isNullOrEmpty()) {
        throw FileNotFoundException()
    }
    return resolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
        ?.createInputStream()
}

fun saveFile(
    context: Context,
    targeturi: Uri,
    sourceuri: Uri,
): Boolean {
    val bis: BufferedInputStream?
    var bos: BufferedOutputStream? = null
    val input: InputStream?
    var hasError = false
    try {
        input = if (isVirtualFile(context, sourceuri)) {
            getInputStreamForVirtualFile(context, sourceuri)
        } else {
            context.contentResolver.openInputStream(sourceuri)
        }
        val output = context.contentResolver.openOutputStream(targeturi)

        bis = BufferedInputStream(input)
        bos = BufferedOutputStream(output)

        val buf = ByteArray(Utils.BUFFER_SIZE)
        var numBytes = bis.read(buf)
        while (numBytes != -1) {
            bos.write(buf, 0, numBytes)
            numBytes = bis.read(buf)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hasError = true
    } finally {
        try {
            if (bos != null) {
                bos.flush()
                bos.close()
            }
        } catch (_: Exception) {
        }
    }
    return !hasError
}

