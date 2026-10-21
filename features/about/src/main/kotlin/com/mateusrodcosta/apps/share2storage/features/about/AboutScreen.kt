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

package com.mateusrodcosta.apps.share2storage.features.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mateusrodcosta.apps.share2storage.core.AppConstants
import com.mateusrodcosta.apps.share2storage.core.ui.components.SectionHeader
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import com.mateusrodcosta.apps.share2storage.core.resources.R as RResources

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}

@Preview(apiLevel = 36, device = Devices.PIXEL_TABLET, showSystemUi = true, showBackground = true)
@Composable
fun AboutScreenPreviewLandscape() {
    AboutScreen()
}

@Composable
fun AboutScreenHeader(
    modifier: Modifier = Modifier,
    versionName: String = "",
    appIcon: Painter? = null
) {
    val uriHandler = LocalUriHandler.current

    val appName = stringResource(RResources.string.app_name)
    val developerCredits = stringResource(RResources.string.about_developer, AppConstants.APP_DEVELOPER)
    val iconCredits = stringResource(RResources.string.about_icon_credits, AppConstants.APP_ICONS_DESIGNER)
    val githubRepoText = stringResource(RResources.string.about_github, AppConstants.GITHUB_REPO)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                if (appIcon != null) {
                    Icon(
                        painter = appIcon,
                        contentDescription = appName,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (versionName.isNotEmpty()) {
                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = developerCredits,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { uriHandler.openUri(AppConstants.GITHUB_PROFILE) }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = iconCredits,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { uriHandler.openUri(AppConstants.SWY_WEBSITE) }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = githubRepoText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { uriHandler.openUri(AppConstants.GITHUB_REPO) }
                .padding(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {},
    versionName: String = "",
    appIcon: Painter? = null
) {
    val libraries by produceLibraries()
    val listState = rememberLazyListState()

    val aboutTitle = stringResource(RResources.string.about_title)
    val backArrowContentDescription = stringResource(RResources.string.back_arrow)
    val licensesCategory = stringResource(RResources.string.about_category_licenses)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(aboutTitle)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backArrowContentDescription
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
            ) {
                LazyColumnScrollbar(
                    state = listState,
                    settings = ScrollbarSettings.Default.copy(
                        thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        thumbSelectedColor = MaterialTheme.colorScheme.primary,
                        alwaysShowScrollbar = true
                    )
                ) {
                    LibrariesContainer(
                        lazyListState = listState,
                        libraries = libraries,
                        header = {
                            item {
                                AboutScreenHeader(
                                    versionName = versionName,
                                    appIcon = appIcon
                                )
                            }
                            item {
                                SectionHeader(licensesCategory)
                            }
                        }
                    )
                }
            }
        }
    }
}
