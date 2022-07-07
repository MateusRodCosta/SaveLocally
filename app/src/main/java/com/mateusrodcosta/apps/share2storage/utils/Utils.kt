/*
 *     Copyright (C) 2022 Mateus Rodrigues Costa
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.mateusrodcosta.apps.share2storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.mateusrodcosta.apps.share2storage.model.UriData
import java.io.*

fun getUriData(contentResolver: ContentResolver, uri: Uri): UriData? {
    contentResolver.query(uri, null, null, null, null)
        ?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            return UriData(cursor.getString(nameIndex), cursor.getLong(sizeIndex))
        }
    return null
}

private fun isVirtualFile(context: Context, uri: Uri): Boolean {

    if (!DocumentsContract.isDocumentUri(context, uri)) {
        return false
    }
    val cursor: Cursor = context.contentResolver.query(
        uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
        null, null, null
    ) ?: return false
    var flags = 0
    if (cursor.moveToFirst()) {
        flags = cursor.getInt(0)
    }
    cursor.close()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }
    return false
}

@Throws(IOException::class)
private fun getInputStreamForVirtualFile(
    context: Context,
    uri: Uri,
): InputStream? {
    val resolver = context.contentResolver
    val filter = "*/*"
    val openableMimeTypes = resolver.getStreamTypes(uri, filter)
    if (openableMimeTypes == null || openableMimeTypes.isEmpty()) {
        throw FileNotFoundException()
    }
    return resolver
        .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
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

        val originalsize: Int = input!!.available()
        bis = BufferedInputStream(input)
        bos = BufferedOutputStream(output)
        val buf = ByteArray(originalsize)
        bis.read(buf)
        do {
            bos.write(buf)
        } while (bis.read(buf) != -1)

    } catch (e: Exception) {
        e.printStackTrace()
        hasError = true
    } finally {
        try {
            if (bos != null) {
                bos.flush()
                bos.close()
            }
        } catch (ignored: Exception) {
        }
    }
    return !hasError
}
