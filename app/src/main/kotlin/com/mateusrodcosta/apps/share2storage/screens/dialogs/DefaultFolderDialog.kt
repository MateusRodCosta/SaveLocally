/*
 *     Copyright (C) 2024 - 2026 Mateus Rodrigues Costa
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

package com.mateusrodcosta.apps.share2storage.screens.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme


@Preview(apiLevel = 36)
@Composable
fun DefaultFolderDialogContentPreview() {
    DefaultFolderDialogContent()
}

@Preview(apiLevel = 36, locale = "pt-rBR")
@Composable
fun DefaultFolderDialogContentPreviewPtBr() {
    DefaultFolderDialogContent()
}

@Composable
fun DefaultFolderDialog(
    onDismissRequest: () -> Unit,
    clearDefaultSaveLocation: () -> Unit = {},
    launchFilePicker: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        DefaultFolderDialogContent(
            onDismissRequest = onDismissRequest,
            clearDefaultSaveLocation = clearDefaultSaveLocation,
            launchFilePicker = launchFilePicker
        )
    }
}

@Composable
fun DefaultFolderDialogContent(
    onDismissRequest: () -> Unit = {},
    clearDefaultSaveLocation: () -> Unit = {},
    launchFilePicker: () -> Unit = {},
) {
    AppTheme {
        Card {
            Column {
                ListItem(modifier = Modifier.clickable(onClick = {
                    clearDefaultSaveLocation()
                    onDismissRequest()
                }),
                    headlineContent = { Text(stringResource(R.string.settings_default_save_location_last_used)) })
                ListItem(modifier = Modifier.clickable(onClick = {
                    launchFilePicker()
                    onDismissRequest()
                }),
                    headlineContent = { Text(stringResource(R.string.settings_default_save_location_select_folder)) })
            }
        }
    }
}
