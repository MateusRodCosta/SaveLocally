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

package com.mateusrodcosta.apps.share2storage.domain.usecases

import com.mateusrodcosta.apps.share2storage.domain.entity.UriData
import com.mateusrodcosta.apps.share2storage.domain.repository.FileRepository
import org.koin.core.annotation.Factory

@Factory
class GetFileMetadataUseCase(private val fileRepository: FileRepository)  {

    suspend operator fun invoke(uriString: String): Result<UriData> {
        return fileRepository.getFileMetadata(uriString)
    }
}
