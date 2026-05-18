/*
 *     Copyright (C) 2022 - 2026 Mateus Rodrigues Costa
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
import androidx.activity.result.contract.ActivityResultContract

class CreateDocumentWithInitialUri :
    ActivityResultContract<CreateDocumentWithInitialUri.Input, Uri?>() {

    data class Input(val fileName: String, val mimeType: String, val initialUri: Uri?)

    override fun createIntent(context: Context, input: Input): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)
            .also { i ->
                Log.d("CreateDocumentWithInitialUri] initialUri", input.initialUri.toString())
                input.initialUri?.let {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        input.initialUri,
                        DocumentsContract.getTreeDocumentId(input.initialUri)
                    )
                    Log.d("CreateDocumentWithInitialUri] documentUri", documentUri.toString())
                    i.putExtra(EXTRA_INITIAL_URI, documentUri)
                }
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == android.app.Activity.RESULT_OK }?.data
    }
}
