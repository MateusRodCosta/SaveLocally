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

package com.mateusrodcosta.apps.share2storage.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.screens.components.BasicDivider
import com.mateusrodcosta.apps.share2storage.screens.components.SectionHeader
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import com.mateusrodcosta.apps.share2storage.screens.components.dialogs.DefaultFolderDialog
import com.mateusrodcosta.apps.share2storage.utils.common.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Preview(apiLevel = 36, showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}


@Preview(apiLevel = 36, showSystemUi = true, showBackground = true, locale = "pt-rBR")
@Composable
fun SettingsScreenPreviewPtBr() {
    SettingsScreen()
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel? = null,
    onBackClick: () -> Unit = {}
) {
    val mockDefaultSaveLocation = remember { MutableStateFlow<String?>(null) }
    val mockSkipFilePicker =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SKIP_FILE_PICKER_DEFAULT) }
    val mockSkipFileDetails =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT) }
    val mockShowFilePreview =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT) }
    val mockInterceptActionViewIntents =
        remember { MutableStateFlow<Boolean?>(PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT) }

    SettingsScreenContent(
        spDefaultSaveLocation = settingsViewModel?.defaultSaveLocation ?: mockDefaultSaveLocation,
        spSkipFilePicker = settingsViewModel?.skipFilePicker ?: mockSkipFilePicker,
        spSkipFileDetails = settingsViewModel?.skipFileDetails ?: mockSkipFileDetails,
        spShowFilePreview = settingsViewModel?.showFilePreview ?: mockShowFilePreview,
        spInterceptActionViewIntents = settingsViewModel?.interceptActionViewIntents
            ?: mockInterceptActionViewIntents,
        onBackClick = onBackClick,
        launchFilePicker = { settingsViewModel?.getSaveLocationDirIntent()?.launch(null) },
        clearDefaultSaveLocation = { settingsViewModel?.clearDefaultSaveLocation() },
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
    onBackClick: () -> Unit = {},
    launchFilePicker: () -> Unit = {},
    clearDefaultSaveLocation: () -> Unit = {},
    updateSkipFilePicker: (Boolean) -> Unit = {},
    updateSkipFileDetails: (Boolean) -> Unit = {},
    updateInterceptActionViewIntents: (Boolean) -> Unit = {},
    updateShowFilePreview: (Boolean) -> Unit = {},
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings_title)) }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.back_arrow),
                )
            }
        })
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column {
                    SectionHeader(stringResource(R.string.settings_category_file_picker))
                    DefaultSaveLocationSetting(
                        launchFilePicker = launchFilePicker,
                        clearDefaultSaveLocation = clearDefaultSaveLocation,
                        spDefaultSaveLocation = spDefaultSaveLocation,
                    )
                    SkipFilePickerSetting(
                        spDefaultSaveLocation = spDefaultSaveLocation,
                        updateSkipFilePicker = updateSkipFilePicker,
                        spSkipFilePicker = spSkipFilePicker,
                    )
                    BasicDivider()
                    SectionHeader(stringResource(R.string.settings_category_file_details))
                    SkipFileDetailsSetting(
                        updateSkipFileDetails = updateSkipFileDetails,
                        spSkipFileDetails = spSkipFileDetails,
                    )
                    ShowFilePreviewSetting(
                        updateShowFilePreview = updateShowFilePreview,
                        spShowFilePreview = spShowFilePreview,
                        spSkipFileDetails = spSkipFileDetails,
                    )
                    BasicDivider()
                    SectionHeader(stringResource(R.string.settings_category_intents))
                    InterceptActionViewIntentsSetting(
                        updateInterceptActionViewIntents = updateInterceptActionViewIntents,
                        spInterceptActionViewIntents = spInterceptActionViewIntents,
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultSaveLocationSetting(
    launchFilePicker: () -> Unit,
    clearDefaultSaveLocation: () -> Unit,
    spDefaultSaveLocation: StateFlow<String?>,
) {
    val defaultSaveLocation by spDefaultSaveLocation.collectAsState()
    val openDefaultFolderDialog = remember { mutableStateOf(false) }

    if (openDefaultFolderDialog.value) {
        DefaultFolderDialog(
            onDismissRequest = {
                openDefaultFolderDialog.value = false
            },
            clearDefaultSaveLocation = clearDefaultSaveLocation,
            launchFilePicker = launchFilePicker,
        )
    }

    ListItem(
        modifier = Modifier.clickable { openDefaultFolderDialog.value = true },
        headlineContent = {
            Text(stringResource(R.string.settings_default_save_location))
        },
        supportingContent = {
            Text(
                defaultSaveLocation?.toUri()?.path
                    ?: stringResource(R.string.settings_default_save_location_last_used)
            )
        },
    )
}

@Composable
fun SkipFilePickerSetting(
    spDefaultSaveLocation: StateFlow<String?>,
    updateSkipFilePicker: (Boolean) -> Unit,
    spSkipFilePicker: StateFlow<Boolean?>,
) {
    val skipFilePicker by spSkipFilePicker.collectAsState()
    val defaultSaveLocation by spDefaultSaveLocation.collectAsState()

    val checked = skipFilePicker ?: PreferencesRepository.SKIP_FILE_PICKER_DEFAULT

    ListItem(modifier = if (defaultSaveLocation != null) Modifier.toggleable(
        value = checked,
        onValueChange = { updateSkipFilePicker(it) },
        role = Role.Switch
    ) else Modifier.alpha(Utils.CONTENT_ALPHA_DISABLED), headlineContent = {
        Text(stringResource(R.string.settings_skip_file_picker))
    }, supportingContent = {
        Text(stringResource(R.string.settings_skip_file_picker_info))
    }, trailingContent = {
        Switch(
            enabled = defaultSaveLocation != null,
            checked = checked,
            onCheckedChange = null,
        )
    })
}

@Composable
fun SkipFileDetailsSetting(
    updateSkipFileDetails: (Boolean) -> Unit,
    spSkipFileDetails: StateFlow<Boolean?>,
) {
    val skipFileDetails by spSkipFileDetails.collectAsState()

    val checked = skipFileDetails ?: PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT

    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = { updateSkipFileDetails(it) },
            role = Role.Switch
        ),
        headlineContent = {
            Text(stringResource(R.string.settings_skip_file_details_page))
        },
        supportingContent = {
            Text(stringResource(R.string.settings_skip_file_details_page_info))
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        })
}

@Composable
fun ShowFilePreviewSetting(
    spSkipFileDetails: StateFlow<Boolean?>,
    updateShowFilePreview: (Boolean) -> Unit,
    spShowFilePreview: StateFlow<Boolean?>,
) {
    val skipFileDetails by spSkipFileDetails.collectAsState()
    val showFilePreview by spShowFilePreview.collectAsState()

    val checked = showFilePreview ?: PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT

    ListItem(modifier = if (skipFileDetails == true) Modifier.alpha(Utils.CONTENT_ALPHA_DISABLED) else Modifier.toggleable(
        value = checked,
        onValueChange = { updateShowFilePreview(it) },
        role = Role.Switch
    ), headlineContent = {
        Text(stringResource(R.string.settings_show_file_preview))
    }, supportingContent = {
        Text(stringResource(R.string.settings_show_file_preview_info))
    }, trailingContent = {
        Switch(
            enabled = skipFileDetails != true,
            checked = checked,
            onCheckedChange = null
        )
    })
}

@Composable
fun InterceptActionViewIntentsSetting(
    updateInterceptActionViewIntents: (Boolean) -> Unit,
    spInterceptActionViewIntents: StateFlow<Boolean?>,
) {
    val interceptActionViewIntents by spInterceptActionViewIntents.collectAsState()

    val checked = interceptActionViewIntents ?: PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT

    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = { updateInterceptActionViewIntents(it) },
            role = Role.Switch
        ),
        headlineContent = {
            Text(stringResource(R.string.settings_intercept_action_view_intents))
        },
        supportingContent = {
            Text(stringResource(R.string.settings_intercept_action_view_intents_info))
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        })
}
