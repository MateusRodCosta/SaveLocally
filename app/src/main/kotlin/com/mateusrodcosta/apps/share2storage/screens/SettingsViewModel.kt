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

package com.mateusrodcosta.apps.share2storage.screens

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PreferencesRepository) : ViewModel() {

    private lateinit var getSaveLocationDirIntent: ActivityResultLauncher<Uri?>

    val defaultSaveLocation: StateFlow<String?> = repository.defaultSaveLocation.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), null
    )
    val skipFileDetails: StateFlow<Boolean> = repository.skipFileDetails.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT
    )
    val showFilePreview: StateFlow<Boolean> = repository.showFilePreview.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT
    )
    val interceptActionViewIntents: StateFlow<Boolean> =
        repository.interceptActionViewIntents.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT
        )
    val skipFilePicker: StateFlow<Boolean> = repository.skipFilePicker.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        PreferencesRepository.SKIP_FILE_PICKER_DEFAULT
    )

    fun assignSaveLocationDirIntent(intent: ActivityResultLauncher<Uri?>) {
        getSaveLocationDirIntent = intent
    }

    fun getSaveLocationDirIntent(): ActivityResultLauncher<Uri?> {
        return getSaveLocationDirIntent
    }

    fun updateDefaultSaveLocation(value: Uri?) {
        viewModelScope.launch {
            repository.setDefaultSaveLocation(value?.toString())
        }
    }

    fun clearDefaultSaveLocation() {
        updateDefaultSaveLocation(null)
    }

    fun updateSkipFileDetails(value: Boolean) {
        viewModelScope.launch {
            repository.setSkipFileDetails(value)
        }
    }

    fun updateInterceptActionViewIntents(value: Boolean) {
        viewModelScope.launch {
            repository.setInterceptActionViewIntents(value)
        }
    }

    fun updateShowFilePreview(value: Boolean) {
        viewModelScope.launch {
            repository.setShowFilePreview(value)
        }
    }

    fun updateSkipFilePicker(value: Boolean) {
        viewModelScope.launch {
            repository.setSkipFilePicker(value)
        }
    }
}