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

package com.mateusrodcosta.apps.share2storage.features.settings.components.dialogs

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.mateusrodcosta.apps.share2storage.core.ui.theme.SaveLocallyTheme

data class DefaultFolderDialogStrings(
    val lastUsed: String = "Last used location",
    val downloads: String = "Downloads folder",
    val selectFolder: String = "Select another folder..."
)

@Preview(apiLevel = 36)
@Composable
fun DefaultFolderDialogContentPreview() {
    DefaultFolderDialogContent()
}

@Composable
fun DefaultFolderDialog(
    onDismissRequest: () -> Unit,
    clearDefaultSaveLocation: () -> Unit = {},
    selectDownloadsFolder: () -> Unit = {},
    launchFilePicker: () -> Unit = {},
    strings: DefaultFolderDialogStrings = DefaultFolderDialogStrings()
) {
    Dialog(onDismissRequest = onDismissRequest) {
        DefaultFolderDialogContent(
            onDismissRequest = onDismissRequest,
            clearDefaultSaveLocation = clearDefaultSaveLocation,
            selectDownloadsFolder = selectDownloadsFolder,
            launchFilePicker = launchFilePicker,
            strings = strings
        )
    }
}

@Composable
fun DefaultFolderDialogContent(
    onDismissRequest: () -> Unit = {},
    clearDefaultSaveLocation: () -> Unit = {},
    selectDownloadsFolder: () -> Unit = {},
    launchFilePicker: () -> Unit = {},
    strings: DefaultFolderDialogStrings = DefaultFolderDialogStrings()
) {
    SaveLocallyTheme {
        Card {
            Column {
                ListItem(
                    modifier = Modifier.clickable(onClick = {
                        clearDefaultSaveLocation()
                        onDismissRequest()
                    }),
                    headlineContent = { Text(strings.lastUsed) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ListItem(
                        modifier = Modifier.clickable(onClick = {
                            selectDownloadsFolder()
                            onDismissRequest()
                        }),
                        headlineContent = { Text(strings.downloads) }
                    )
                }
                ListItem(
                    modifier = Modifier.clickable(onClick = {
                        launchFilePicker()
                        onDismissRequest()
                    }),
                    headlineContent = { Text(strings.selectFolder) }
                )
            }
        }
    }
}
