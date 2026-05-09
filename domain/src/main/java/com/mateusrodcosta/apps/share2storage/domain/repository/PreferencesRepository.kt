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

package com.mateusrodcosta.apps.share2storage.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    companion object {
        const val SKIP_FILE_PICKER_DEFAULT: Boolean = false
        const val SKIP_FILE_DETAILS_DEFAULT: Boolean = true
        const val SHOW_FILE_PREVIEW_DEFAULT: Boolean = false
        const val INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT: Boolean = false
    }

    val defaultSaveLocation: Flow<String?>
    val skipFileDetails: Flow<Boolean>
    val showFilePreview: Flow<Boolean>
    val interceptActionViewIntents: Flow<Boolean>
    val skipFilePicker: Flow<Boolean>

    suspend fun setDefaultSaveLocation(saveLocation: String?)
    suspend fun setSkipFileDetails(skipDetails: Boolean)
    suspend fun setShowFilePreview(showPreview: Boolean)
    suspend fun setInterceptActionViewIntents(interceptIntents: Boolean)
    suspend fun setSkipFilePicker(skipPicker: Boolean)
}
