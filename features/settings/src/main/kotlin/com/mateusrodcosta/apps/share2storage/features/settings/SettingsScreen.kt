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

package com.mateusrodcosta.apps.share2storage.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.core.AppConstants
import com.mateusrodcosta.apps.share2storage.core.ui.components.SectionHeader
import com.mateusrodcosta.apps.share2storage.core.ui.utils.Utils
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import com.mateusrodcosta.apps.share2storage.features.settings.components.dialogs.DefaultFolderDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mateusrodcosta.apps.share2storage.core.resources.R as RResources

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}

@Preview(apiLevel = 36, device = Devices.PIXEL_TABLET, showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreviewLandscape() {
    SettingsScreen()
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel? = null,
    onNavigateToAbout: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val selectFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            settingsViewModel?.updateDefaultSaveLocation(uri)
        }
    }

    val mockDefaultSaveLocation = remember { MutableStateFlow<String?>(null) }
    val mockSkipFilePicker =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SKIP_FILE_PICKER_DEFAULT) }
    val mockSkipFileDetails =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT) }
    val mockShowFilePreview =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT) }
    val mockInterceptActionViewIntents =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT) }

    val listItemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
        leadingIconColor = MaterialTheme.colorScheme.primary
    )

    SettingsScreenContent(
        spDefaultSaveLocation = settingsViewModel?.defaultSaveLocation ?: mockDefaultSaveLocation,
        spSkipFilePicker = settingsViewModel?.skipFilePicker ?: mockSkipFilePicker,
        spSkipFileDetails = settingsViewModel?.skipFileDetails ?: mockSkipFileDetails,
        spShowFilePreview = settingsViewModel?.showFilePreview ?: mockShowFilePreview,
        spInterceptActionViewIntents = settingsViewModel?.interceptActionViewIntents
            ?: mockInterceptActionViewIntents,
        onNavigateToAbout = onNavigateToAbout,
        onBackClick = onBackClick,
        launchFilePicker = { selectFolderLauncher.launch(null) },
        clearDefaultSaveLocation = { settingsViewModel?.clearDefaultSaveLocation() },
        selectDownloadsFolder = { settingsViewModel?.updateDefaultSaveLocationDownloads() },
        updateSkipFilePicker = { value: Boolean ->
            settingsViewModel?.updateSkipFilePicker(value)
        },
        updateSkipFileDetails = { value: Boolean ->
            settingsViewModel?.updateSkipFileDetails(value)
        },
        updateInterceptActionViewIntents = { value: Boolean ->
            settingsViewModel?.updateInterceptActionViewIntents(value)
        },
        updateShowFilePreview = { value ->
            settingsViewModel?.updateShowFilePreview(value)
        },
        listItemColors = listItemColors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    spDefaultSaveLocation: StateFlow<String?>,
    spSkipFilePicker: StateFlow<Boolean?>,
    spSkipFileDetails: StateFlow<Boolean?>,
    spShowFilePreview: StateFlow<Boolean?>,
    spInterceptActionViewIntents: StateFlow<Boolean?>,
    onNavigateToAbout: () -> Unit = {},
    onBackClick: () -> Unit = {},
    launchFilePicker: () -> Unit = {},
    clearDefaultSaveLocation: () -> Unit = {},
    selectDownloadsFolder: () -> Unit = {},
    updateSkipFilePicker: (Boolean) -> Unit = {},
    updateSkipFileDetails: (Boolean) -> Unit = {},
    updateInterceptActionViewIntents: (Boolean) -> Unit = {},
    updateShowFilePreview: (Boolean) -> Unit = {},
    listItemColors: ListItemColors = ListItemDefaults.colors()
) {
    val settingsTitle = stringResource(RResources.string.settings_title)
    val backArrow = stringResource(RResources.string.back_arrow)
    val categoryFilePicker = stringResource(RResources.string.settings_category_file_picker)
    val categoryFileDetails = stringResource(RResources.string.settings_category_file_details)
    val categoryIntents = stringResource(RResources.string.settings_category_intents)
    val categoryAbout = stringResource(RResources.string.settings_category_about)
    val aboutTitle = stringResource(RResources.string.settings_about)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(settingsTitle) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        backArrow,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                SectionHeader(categoryFilePicker)
                DefaultSaveLocationSetting(
                    launchFilePicker = launchFilePicker,
                    clearDefaultSaveLocation = clearDefaultSaveLocation,
                    selectDownloadsFolder = selectDownloadsFolder,
                    spDefaultSaveLocation = spDefaultSaveLocation,
                    listItemColors = listItemColors
                )
                SkipFilePickerSetting(
                    spDefaultSaveLocation = spDefaultSaveLocation,
                    updateSkipFilePicker = updateSkipFilePicker,
                    spSkipFilePicker = spSkipFilePicker,
                    listItemColors = listItemColors
                )
                SectionHeader(categoryFileDetails)
                SkipFileDetailsSetting(
                    updateSkipFileDetails = updateSkipFileDetails,
                    spSkipFileDetails = spSkipFileDetails,
                    listItemColors = listItemColors
                )
                ShowFilePreviewSetting(
                    updateShowFilePreview = updateShowFilePreview,
                    spShowFilePreview = spShowFilePreview,
                    spSkipFileDetails = spSkipFileDetails,
                    listItemColors = listItemColors
                )
                SectionHeader(categoryIntents)
                InterceptActionViewIntentsSetting(
                    updateInterceptActionViewIntents = updateInterceptActionViewIntents,
                    spInterceptActionViewIntents = spInterceptActionViewIntents,
                    listItemColors = listItemColors
                )
                SectionHeader(categoryAbout)
                ListItem(
                    headlineContent = {
                        Text(aboutTitle)
                    },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = aboutTitle
                        )
                    },
                    colors = listItemColors,
                    modifier = Modifier.clickable(onClick = onNavigateToAbout)
                )
            }
        }
    }
}

@Composable
fun DefaultSaveLocationSetting(
    launchFilePicker: () -> Unit,
    clearDefaultSaveLocation: () -> Unit,
    selectDownloadsFolder: () -> Unit = {},
    spDefaultSaveLocation: StateFlow<String?>,
    listItemColors: ListItemColors
) {
    val defaultSaveLocation by spDefaultSaveLocation.collectAsState()
    val openDefaultFolderDialog = remember { mutableStateOf(false) }

    val defaultSaveLocationLabel = stringResource(RResources.string.settings_default_save_location)
    val lastUsedLabel = stringResource(RResources.string.settings_default_save_location_last_used)
    val downloadsLabel = stringResource(RResources.string.settings_default_save_location_downloads)

    if (openDefaultFolderDialog.value) {
        DefaultFolderDialog(
            onDismissRequest = {
                openDefaultFolderDialog.value = false
            },
            clearDefaultSaveLocation = clearDefaultSaveLocation,
            selectDownloadsFolder = selectDownloadsFolder,
            launchFilePicker = launchFilePicker
        )
    }

    ListItem(
        modifier = Modifier.clickable { openDefaultFolderDialog.value = true },
        headlineContent = {
            Text(defaultSaveLocationLabel)
        },
        supportingContent = {
            val text = when (defaultSaveLocation) {
                null -> lastUsedLabel
                AppConstants.MEDIASTORE_DOWNLOADS_URI -> downloadsLabel
                else -> defaultSaveLocation?.toUri()?.path ?: lastUsedLabel
            }
            Text(text)
        },
        leadingContent = {
            Icon(Icons.Default.Folder, null)
        },
        colors = listItemColors,
    )
}

@Composable
fun SkipFilePickerSetting(
    spDefaultSaveLocation: StateFlow<String?>,
    updateSkipFilePicker: (Boolean) -> Unit,
    spSkipFilePicker: StateFlow<Boolean?>,
    listItemColors: ListItemColors
) {
    val skipFilePicker by spSkipFilePicker.collectAsState()
    val defaultSaveLocation by spDefaultSaveLocation.collectAsState()

    val title = stringResource(RResources.string.settings_skip_file_picker)
    val info = stringResource(RResources.string.settings_skip_file_picker_info)

    val checked = if (defaultSaveLocation == null) {
        false
    } else {
        skipFilePicker ?: PreferencesRepository.SKIP_FILE_PICKER_DEFAULT
    }
    val isDownloadsFolder = defaultSaveLocation == AppConstants.MEDIASTORE_DOWNLOADS_URI
    val isEnabled = defaultSaveLocation != null && !isDownloadsFolder

    ListItem(
        modifier = if (isEnabled) Modifier.toggleable(
            value = checked,
            onValueChange = { updateSkipFilePicker(it) },
            role = Role.Switch
        ) else Modifier,
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(info)
        },
        leadingContent = {
            Icon(Icons.Default.FastForward, null)
        },
        trailingContent = {
            Switch(
                enabled = isEnabled,
                checked = checked,
                onCheckedChange = null,
            )
        },
        colors = listItemColors
    )
}

@Composable
fun SkipFileDetailsSetting(
    updateSkipFileDetails: (Boolean) -> Unit,
    spSkipFileDetails: StateFlow<Boolean?>,
    listItemColors: ListItemColors
) {
    val skipFileDetails by spSkipFileDetails.collectAsState()

    val title = stringResource(RResources.string.settings_skip_file_details_page)
    val info = stringResource(RResources.string.settings_skip_file_details_page_info)

    val checked = skipFileDetails ?: PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT

    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = { updateSkipFileDetails(it) },
            role = Role.Switch
        ),
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(info)
        },
        leadingContent = {
            Icon(Icons.Default.Description, null)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        colors = listItemColors)
}

@Composable
fun ShowFilePreviewSetting(
    spSkipFileDetails: StateFlow<Boolean?>,
    updateShowFilePreview: (Boolean) -> Unit,
    spShowFilePreview: StateFlow<Boolean?>,
    listItemColors: ListItemColors
) {
    val skipFileDetails by spSkipFileDetails.collectAsState()
    val showFilePreview by spShowFilePreview.collectAsState()

    val title = stringResource(RResources.string.settings_show_file_preview)
    val info = stringResource(RResources.string.settings_show_file_preview_info_all_types)

    val checked = showFilePreview ?: PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT

    ListItem(modifier = if (skipFileDetails == true) Modifier.alpha(Utils.CONTENT_ALPHA_DISABLED) else Modifier.toggleable(
        value = checked,
        onValueChange = { updateShowFilePreview(it) },
        role = Role.Switch
    ), headlineContent = {
        Text(title)
    }, supportingContent = {
        Text(info)
    }, leadingContent = {
        Icon(Icons.Default.Image, null)
    }, trailingContent = {
        Switch(
            enabled = skipFileDetails != true,
            checked = checked,
            onCheckedChange = null
        )
    }, colors = listItemColors)
}

@Composable
fun InterceptActionViewIntentsSetting(
    updateInterceptActionViewIntents: (Boolean) -> Unit,
    spInterceptActionViewIntents: StateFlow<Boolean?>,
    listItemColors: ListItemColors
) {
    val interceptActionViewIntents by spInterceptActionViewIntents.collectAsState()

    val title = stringResource(RResources.string.settings_intercept_action_view_intents)
    val info = stringResource(RResources.string.settings_intercept_action_view_intents_info)

    val checked = interceptActionViewIntents ?: PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT

    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = { updateInterceptActionViewIntents(it) },
            role = Role.Switch
        ),
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(info)
        },
        leadingContent = {
            Icon(Icons.Default.Link, null)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        colors = listItemColors)
}
