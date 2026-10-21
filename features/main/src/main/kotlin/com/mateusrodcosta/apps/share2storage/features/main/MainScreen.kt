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

package com.mateusrodcosta.apps.share2storage.features.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.mateusrodcosta.apps.share2storage.core.ui.utils.shouldShowLandscape
import com.mateusrodcosta.apps.share2storage.core.resources.R as RResources

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}

@Preview(apiLevel = 36, device = Devices.PIXEL_TABLET, showSystemUi = true, showBackground = true)
@Composable
fun MainScreenPreviewLandscape() {
    MainScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    openSettings: () -> Unit = {},
    headerIcon: Painter? = null
) {
    val useLandscapeLayout = shouldShowLandscape(windowSizeClass)
    val appName = stringResource(RResources.string.app_name)
    val settingsTitle = stringResource(RResources.string.settings_title)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(appName) },
            actions = {
                IconButton(onClick = openSettings) {
                    Icon(Icons.Rounded.Settings, settingsTitle)
                }
            },
        )
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (useLandscapeLayout) HowToUseLandscape(headerIcon = headerIcon)
            else HowToUsePortrait(headerIcon = headerIcon)
        }
    }
}

@Composable
fun HowToUseLandscape(
    headerIcon: Painter? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1.0f)) {
            HowToUseHeader(headerIcon = headerIcon)
        }
        Box(modifier = Modifier.weight(3.0f)) {
            HowToUseContent(isLandscape = true)
        }
    }
}

@Composable
fun HowToUsePortrait(
    headerIcon: Painter? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        HowToUseHeader(headerIcon = headerIcon)
        HowToUseContent()
    }
}

@Composable
fun HowToUseHeader(
    headerIcon: Painter? = null
) {
    val appName = stringResource(RResources.string.app_name)
    val howToUseTitle = stringResource(RResources.string.how_to_use)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (headerIcon != null) {
            Image(
                painter = headerIcon,
                contentDescription = appName,
                modifier = Modifier.scale(1.5f),
            )
        }
        Text(
            text = howToUseTitle,
            style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.secondary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HowToUseContent(
    isLandscape: Boolean = false
) {
    val appName = stringResource(RResources.string.app_name)
    val step1 = stringResource(RResources.string.how_to_use_step_1)
    val step2 = stringResource(RResources.string.how_to_use_step_2, appName)
    val step3 = stringResource(RResources.string.how_to_use_step_3)
    val step4 = stringResource(RResources.string.how_to_use_step_4)

    Column(
        modifier = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()
    ) {
        HowToUseRow(1, step1)
        HowToUseRow(2, step2)
        HowToUseRow(3, step3)
        HowToUseRow(4, step4)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HowToUseRow(num: Int?, string: String) {
    ListItem(leadingContent = {
        Text(
            if (num == null) "   " else "$num.",
            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
        )
    }, headlineContent = {
        Text(string, softWrap = true)
    })
}
