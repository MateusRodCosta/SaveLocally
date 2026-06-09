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

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mateusrodcosta.apps.share2storage.navigation.Routes
import com.mateusrodcosta.apps.share2storage.screens.AboutScreen
import com.mateusrodcosta.apps.share2storage.screens.MainScreen
import com.mateusrodcosta.apps.share2storage.screens.SettingsScreen
import com.mateusrodcosta.apps.share2storage.screens.SettingsViewModel
import com.mateusrodcosta.apps.share2storage.ui.theme.SaveLocallyTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    private val getSaveLocationDirIntent =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            when (uri) {
                null -> Log.d(
                    "MainActivity] getSaveLocationDirIntent] uri",
                    "cancelled directory selection",
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsViewModel.assignSaveLocationDirIntent(getSaveLocationDirIntent)

        splashScreen.setKeepOnScreenCondition {
            !settingsViewModel.isReady.value
        }

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                View.ALPHA,
                1f,
                0f
            )

            fadeOut.duration = 400L
            fadeOut.interpolator = AnticipateInterpolator()

            fadeOut.doOnEnd {
                splashScreenViewProvider.remove()
            }

            fadeOut.start()
        }

        setContent {
            val isReady by settingsViewModel.isReady.collectAsState()

            if (isReady) {
                val isDarkTheme = isSystemInDarkTheme()

                val barStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }

                LaunchedEffect(isDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = barStyle,
                        navigationBarStyle = barStyle
                    )
                }

                SaveLocallyTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    val isFromAppInfo = intent.action == Intent.ACTION_APPLICATION_PREFERENCES
                    
                    val startDestination = if (isFromAppInfo) Routes.Settings else Routes.Main

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable<Routes.Main> {
                                MainScreen(
                                    openSettings = {
                                        navController.navigate(Routes.Settings)
                                    }
                                )
                            }

                            composable<Routes.Settings> {
                                SettingsScreen(
                                    settingsViewModel = settingsViewModel,
                                    onNavigateToAbout = {
                                        navController.navigate(Routes.About)
                                    },
                                    onBackClick = {
                                        if (isFromAppInfo) finish() else navController.popBackStack()
                                    }
                                )
                            }

                            composable<Routes.About> {
                                AboutScreen(
                                    onBackClick = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
