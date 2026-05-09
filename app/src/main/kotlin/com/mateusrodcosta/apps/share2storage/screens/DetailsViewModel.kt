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

package com.mateusrodcosta.apps.share2storage.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import com.mateusrodcosta.apps.share2storage.domain.usecases.GetFileMetadataUseCase
import com.mateusrodcosta.apps.share2storage.domain.usecases.SaveFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DetailsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getFileMetadataUseCase: GetFileMetadataUseCase,
    private val saveFileUseCase: SaveFileUseCase
) : ViewModel() {

    val defaultSaveLocation: StateFlow<String?> = preferencesRepository.defaultSaveLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val skipFilePicker: StateFlow<Boolean> = preferencesRepository.skipFilePicker
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.SKIP_FILE_PICKER_DEFAULT)

    val skipFileDetails: StateFlow<Boolean> = preferencesRepository.skipFileDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT)

    val showFilePreview: StateFlow<Boolean> = preferencesRepository.showFilePreview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT)

    private val _uriData = MutableStateFlow<UriData?>(null)
    val uriData: StateFlow<UriData?> = _uriData.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    fun loadMetadata(uriString: String) {
        viewModelScope.launch {
            getFileMetadataUseCase(uriString).onSuccess {
                _uriData.value = it
            }
        }
    }

    fun saveFile(sourceUriString: String, targetUriString: String) {
        viewModelScope.launch {
            _saveResult.value = saveFileUseCase.saveFile(sourceUriString, targetUriString)
        }
    }

    fun saveText(text: String, targetUriString: String) {
        viewModelScope.launch {
            _saveResult.value = saveFileUseCase.saveText(text, targetUriString)
        }
    }
    
    fun resetSaveResult() {
        _saveResult.value = null
    }
}
