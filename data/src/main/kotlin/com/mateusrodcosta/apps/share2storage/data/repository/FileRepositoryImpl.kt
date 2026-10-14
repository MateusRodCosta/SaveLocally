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
import android.util.Log
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.exception.InsufficientStorageException
import com.mateusrodcosta.apps.share2storage.domain.repository.FileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

@Single
class FileRepositoryImpl(private val context: Context) : FileRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        private const val BUFFER_SIZE = 65536 // 64 KB
    }

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
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesCopied = 0L
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } >= 0) {
                            coroutineContext.ensureActive()
                            outputStream.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            onProgress?.invoke(bytesCopied, size)
                        }
                    } ?: throw IOException("Could not open input stream: $sourceUri")
                } ?: throw IOException("Could not open target output stream for URI: $targetUri")
                Unit
            }.onFailure { e ->
                if (e is CancellationException) throw e
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

    override suspend fun saveZippedFiles(
        files: List<Pair<String, String>>,
        targetZipUriString: String,
        onProgress: ((bytesCopied: Long, totalBytes: Long, currentFileName: String) -> Unit)?
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetZipUri = targetZipUriString.toUri()

                val totalInputBytes = files.sumOf { (sourceUriString, _) ->
                    getFileMetadata(sourceUriString).getOrNull()?.size ?: 0L
                }
                if (totalInputBytes > 0L) {
                    val availableFreeBytes = getAvailableFreeBytes(targetZipUri)
                    if (availableFreeBytes in 0..<totalInputBytes) {
                        throw InsufficientStorageException(requiredBytes = totalInputBytes, availableBytes = availableFreeBytes)
                    }
                }

                val outputStream = contentResolver.openOutputStream(targetZipUri)
                    ?: throw IOException("Could not open target output stream for ZIP URI: $targetZipUri")

                var totalBytesWritten = 0L
                val buffer = ByteArray(BUFFER_SIZE)
                val usedEntryNames = mutableSetOf<String>()

                val bufferedOut = java.io.BufferedOutputStream(outputStream)
                val zipOut = java.util.zip.ZipOutputStream(bufferedOut)

                try {
                    files.forEach { (sourceUriString, rawFileName) ->
                        val sourceUri = sourceUriString.toUri()
                        val inputStream = if (isVirtualFile(sourceUri)) {
                            runCatching { getInputStreamForVirtualFile(sourceUri) }.onFailure { e ->
                                Log.e("FileRepositoryImpl", "Could not open virtual file input stream for: $sourceUriString", e)
                            }.getOrNull()
                        } else {
                            runCatching { contentResolver.openInputStream(sourceUri) }.onFailure { e ->
                                Log.e("FileRepositoryImpl", "Could not open input stream for: $sourceUriString", e)
                            }.getOrNull()
                        }

                        if (inputStream != null) {
                            inputStream.use { input ->
                                val sanitizedName = rawFileName.replace('/', '_').replace('\\', '_').ifBlank { "file" }
                                val uniqueName = getUniqueZipEntryName(sanitizedName, usedEntryNames)
                                usedEntryNames.add(uniqueName)

                                val zipEntry = java.util.zip.ZipEntry(uniqueName).apply {
                                    time = System.currentTimeMillis()
                                }
                                zipOut.putNextEntry(zipEntry)

                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } >= 0) {
                                    coroutineContext.ensureActive()
                                    zipOut.write(buffer, 0, bytesRead)
                                    totalBytesWritten += bytesRead
                                    onProgress?.invoke(totalBytesWritten, totalInputBytes, uniqueName)
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }
                    zipOut.finish()
                    zipOut.flush()
                    bufferedOut.flush()
                } finally {
                    runCatching { zipOut.close() }
                    runCatching { bufferedOut.close() }
                    runCatching { outputStream.close() }
                }
                Unit
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }

    private fun getUniqueZipEntryName(fileName: String, usedNames: Set<String>): String {
        if (!usedNames.contains(fileName)) return fileName

        val dotIndex = fileName.lastIndexOf('.')
        val nameWithoutExtension = if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex != -1) fileName.substring(dotIndex) else ""

        var counter = 1
        var candidateName: String
        do {
            candidateName = "$nameWithoutExtension ($counter)$extension"
            counter++
        } while (usedNames.contains(candidateName))

        return candidateName
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
