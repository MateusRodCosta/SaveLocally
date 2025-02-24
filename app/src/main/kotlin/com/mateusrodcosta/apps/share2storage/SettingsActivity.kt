/*
 *     Copyright (C) 2025 Mateus Rodrigues Costa
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

package com.mateusrodcosta.apps.share2storage

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.mateusrodcosta.apps.share2storage.screens.SettingsScreen
import com.mateusrodcosta.apps.share2storage.screens.SettingsViewModel

class SettingsActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel = SettingsViewModel()

    private val getSaveLocationDirIntent =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            when (uri) {
                null -> Log.i("settings] getSaveLocationDir] uri", "cancelled directory selection")
                else -> {
                    Log.i("settings] getSaveLocationDir] uri", uri.toString())
                    Log.i("settings] getSaveLocationDir] uri.path", uri.path.toString())

                    settingsViewModel.updateDefaultSaveLocation(uri)
                }
            }
        }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsViewModel.initializeWithContext(applicationContext)
        settingsViewModel.assignSaveLocationDirIntent(getSaveLocationDirIntent)
        settingsViewModel.initPreferences()

        setContent {
            SettingsScreen(settingsViewModel)
        }
    }
}
