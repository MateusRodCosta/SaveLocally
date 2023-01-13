/*
 *     Copyright (C) 2022 - 2023 Mateus Rodrigues Costa
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


package com.mateusrodcosta.apps.share2storage.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class SampleUriDataProvider : PreviewParameterProvider<UriData?> {
    override val values = sequenceOf(
        UriData(
            "21. Setting Sail, Coming Home (End Theme).flac",
            "audio/flac",
            35280673
        ),
        UriData("03. Lonely Rolling Star (Missing You).flac", "audio/flac", 41123343),
        UriData(null, null, null),
        null,
    )

}