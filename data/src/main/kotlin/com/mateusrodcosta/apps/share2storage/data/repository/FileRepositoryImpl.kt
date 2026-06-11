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

package com.mateusrodcosta.apps.share2storage.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

@Single
class FileRepositoryImpl(private val context: Context) : FileRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun saveFile(sourceUriString: String, targetUriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceUri = sourceUriString.toUri()
                val targetUri = targetUriString.toUri()

                val inputStream = if (isVirtualFile(sourceUri)) {
                    getInputStreamForVirtualFile(sourceUri)
                } else {
                    contentResolver.openInputStream(sourceUri)
                }

                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    inputStream?.use { it.copyTo(outputStream) }
                        ?: throw IOException("Could not open source input stream")
                } ?: throw IOException("Could not open target output stream")
                Unit
            }
        }

    override suspend fun saveText(text: String, targetUriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetUri = targetUriString.toUri()
                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    text.byteInputStream().copyTo(outputStream)
                } ?: throw IOException("Could not open target output stream")
                Unit
            }
        }

    override suspend fun getFileMetadata(uriString: String): Result<UriData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = uriString.toUri()
                val type = contentResolver.getType(uri) ?: "*/*"

                val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
                val cursor = contentResolver.query(uri, projection, null, null, null)
                    ?: throw IOException("Could not query metadata for URI")

                val (displayName, size) = cursor.use {
                    if (!it.moveToFirst()) throw IOException("Empty cursor for metadata")
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex != -1) it.getString(nameIndex) else "unknown"
                    val s = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                    name to s
                }

                UriData(uriString, displayName, type, size)
            }
        }

    private fun isVirtualFile(uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(context, uri)) return false

        val cursor: Cursor = contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null
        ) ?: return false

        val flags = cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }

    @Throws(IOException::class)
    private fun getInputStreamForVirtualFile(
        uri: Uri,
    ): InputStream? {
        val openableMimeTypes = contentResolver.getStreamTypes(uri, "*/*")
        if (openableMimeTypes.isNullOrEmpty()) throw FileNotFoundException()

        return contentResolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
            ?.createInputStream()
    }
}
