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
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.exception.InsufficientStorageException
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

    override suspend fun saveFile(
        sourceUriString: String,
        targetUriString: String,
        totalBytes: Long,
        onProgress: ((bytesCopied: Long, totalBytes: Long) -> Unit)?
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceUri = sourceUriString.toUri()
                val targetUri = targetUriString.toUri()
                val size = totalBytes.takeIf { it > 0L }
                    ?: throw IllegalArgumentException("Invalid totalBytes ($totalBytes) for URI: $sourceUriString")

                val availableFreeBytes = getAvailableFreeBytes(targetUri)
                if (availableFreeBytes in 0..<size) {
                    throw InsufficientStorageException(requiredBytes = size, availableBytes = availableFreeBytes)
                }

                val inputStream = if (isVirtualFile(sourceUri)) {
                    getInputStreamForVirtualFile(sourceUri)
                } else {
                    contentResolver.openInputStream(sourceUri)
                }

                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    inputStream?.use { input ->
                        val buffer = ByteArray(65536)
                        var bytesCopied = 0L
                        var bytesRead = input.read(buffer)
                        while (bytesRead >= 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            onProgress?.invoke(bytesCopied, size)
                            bytesRead = input.read(buffer)
                        }
                    } ?: throw IOException("Could not open input stream: $sourceUri")
                } ?: throw IOException("Could not open target output stream for URI: $targetUri")
                Unit
            }
        }

    override suspend fun saveText(text: String, targetUriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetUri = targetUriString.toUri()
                contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    outputStream.write(text.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Could not open target output stream for URI: $targetUri")
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
                    ?: throw IOException("Could not query metadata for URI: $uri")

                val (displayName, size) = cursor.use {
                    if (!it.moveToFirst()) throw IOException("Empty cursor for metadata of URI: $uri")
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "unknown" else "unknown"
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

    private fun getAvailableFreeBytes(uri: Uri): Long {
        val safBytes = runCatching {
            contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                val stats = Os.fstatvfs(pfd.fileDescriptor)
                stats.f_bavail * stats.f_frsize
            }
        }.getOrNull()

        if (safBytes != null && safBytes > 0L) {
            return safBytes
        }

        return runCatching {
            val statFs = StatFs(Environment.getExternalStorageDirectory().path)
            statFs.availableBytes
        }.getOrDefault(-1L)
    }

    @Throws(IOException::class)
    private fun getInputStreamForVirtualFile(
        uri: Uri,
    ): InputStream? {
        val openableMimeTypes = contentResolver.getStreamTypes(uri, "*/*")
        if (openableMimeTypes.isNullOrEmpty()) throw FileNotFoundException("No stream types found for virtual file: $uri")

        return contentResolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
            ?.createInputStream()
    }
}
