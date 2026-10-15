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

package com.mateusrodcosta.apps.share2storage.features.details.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import com.mateusrodcosta.apps.share2storage.domain.usecases.CreateTargetUriUseCase
import com.mateusrodcosta.apps.share2storage.domain.usecases.GetFileMetadataUseCase
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
    private val createTargetUriUseCase: CreateTargetUriUseCase
) : ViewModel() {

    val defaultSaveLocation: StateFlow<String?> = preferencesRepository.defaultSaveLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val skipFilePicker: StateFlow<Boolean?> = preferencesRepository.skipFilePicker
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val skipFileDetails: StateFlow<Boolean?> = preferencesRepository.skipFileDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val showFilePreview: StateFlow<Boolean?> = preferencesRepository.showFilePreview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uriData = MutableStateFlow<UriData?>(null)
    val uriData: StateFlow<UriData?> = _uriData.asStateFlow()

    private val _uriDataList = MutableStateFlow<List<UriData>?>(null)
    val uriDataList: StateFlow<List<UriData>?> = _uriDataList.asStateFlow()

    fun loadMetadata(uriString: String) {
        viewModelScope.launch {
            getFileMetadataUseCase(uriString).onSuccess {
                _uriData.value = it
            }
        }
    }

    fun loadMetadataList(uriStrings: List<String>) {
        viewModelScope.launch {
            val list = uriStrings.mapNotNull { uriString ->
                getFileMetadataUseCase(uriString).getOrNull()
            }
            if (list.isNotEmpty()) {
                _uriDataList.value = list
                _uriData.value = list.firstOrNull()
            }
        }
    }

    suspend fun createTargetUri(
        locationUriString: String,
        displayName: String,
        mimeType: String
    ): Result<String> {
        return createTargetUriUseCase(locationUriString, displayName, mimeType)
    }
}
