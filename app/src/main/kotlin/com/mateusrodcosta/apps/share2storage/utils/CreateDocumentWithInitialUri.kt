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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument

class CreateDocumentWithInitialUri(
    mimeType: String, private val initialUri: Uri?
) : CreateDocument(mimeType) {

    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).also { i ->
            Log.d("CreateDocumentWithInitialUri] initialUri", initialUri.toString())
            initialUri?.let {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(initialUri, DocumentsContract.getTreeDocumentId(initialUri))
                Log.d("CreateDocumentWithInitialUri] documentUri", documentUri.toString())
                i.putExtra(EXTRA_INITIAL_URI, documentUri)
            }
        }
    }
}