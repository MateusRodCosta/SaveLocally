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

package com.mateusrodcosta.apps.share2storage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mateusrodcosta.apps.share2storage.screens.MainScreen
import com.mateusrodcosta.apps.share2storage.screens.SettingsScreen
import com.mateusrodcosta.apps.share2storage.screens.SettingsViewModel
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    private val getSaveLocationDirIntent =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            when (uri) {
                null -> Log.d(
                    "MainActivity] getSaveLocationDirIntent] uri",
                    "cancelled directory selection"
                )

                else -> {
                    Log.d("MainActivity] getSaveLocationDirIntent] uri", uri.toString())
                    Log.d(
                        "MainActivity] getSaveLocationDirIntent] uri.path", uri.path.toString()
                    )

                    settingsViewModel.updateDefaultSaveLocation(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsViewModel.assignSaveLocationDirIntent(getSaveLocationDirIntent)

        setContent {
            AppTheme {
                val isFromAppInfo =
                    intent.action == Intent.ACTION_APPLICATION_PREFERENCES
                var showSettings by rememberSaveable { mutableStateOf(isFromAppInfo) }

                BackHandler(enabled = showSettings) {
                    if (isFromAppInfo) finish() else showSettings = false
                }

                Crossfade(targetState = showSettings, label = "MainContentNav") { targetShowSettings ->
                    if (targetShowSettings) {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onBackClick = {
                                if (isFromAppInfo) finish() else showSettings = false
                            }
                        )
                    } else {
                        MainScreen(
                            openSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
