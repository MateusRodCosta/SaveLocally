/*
 *     Copyright (C) 2022 - 2025 Mateus Rodrigues Costa
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

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.mateusrodcosta.apps.share2storage.utils.SharedPreferenceKeys
import com.mateusrodcosta.apps.share2storage.utils.SharedPreferenceUtils
import com.mateusrodcosta.apps.share2storage.utils.SharedPreferencesDefaultValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageManager: PackageManager
    private lateinit var contentResolver: ContentResolver
    private lateinit var getSaveLocationDirIntent: ActivityResultLauncher<Uri?>
    private lateinit var packageName: String

    private val _defaultSaveLocation = MutableStateFlow<Uri?>(null)
    val defaultSaveLocation: StateFlow<Uri?> = _defaultSaveLocation

    private val _skipFileDetails = MutableStateFlow(false)
    val skipFileDetails: StateFlow<Boolean> = _skipFileDetails

    private val _showFilePreview = MutableStateFlow(true)
    val showFilePreview: StateFlow<Boolean> = _showFilePreview

    private val _interceptActionViewIntents = MutableStateFlow(false)
    val interceptActionViewIntents: StateFlow<Boolean> = _interceptActionViewIntents

    private val _skipFilePicker = MutableStateFlow(false)
    val skipFilePicker: StateFlow<Boolean> = _skipFilePicker

    fun assignSaveLocationDirIntent(intent: ActivityResultLauncher<Uri?>) {
        getSaveLocationDirIntent = intent
    }

    fun getSaveLocationDirIntent(): ActivityResultLauncher<Uri?> {
        return getSaveLocationDirIntent
    }

    fun initializeWithContext(context: Context) {
        sharedPreferences = SharedPreferenceUtils.getDefaultSharedPreferences(context)
        contentResolver = context.contentResolver
        packageManager = context.packageManager
        packageName = context.packageName
    }

    fun initPreferences() {
        val spDefaultSaveLocation =
            sharedPreferences.getString(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, null).let {
                Log.d("SettingsViewModel] initPreferences] defaultSaveLocationRaw", it.toString())
                try {
                    Uri.parse(it)
                } catch (_: Exception) {
                    null
                }
            }
        Log.d(
            "SettingsViewModel] initPreferences] defaultSaveLocation",
            spDefaultSaveLocation.toString()
        )
        spDefaultSaveLocation?.path?.let {
            Log.d(
                "SettingsViewModel] initPreferences] defaultSaveLocation.path",
                spDefaultSaveLocation.path.toString()
            )
        }

        val spSkipFilePicker = sharedPreferences.getBoolean(
            SharedPreferenceKeys.SKIP_FILE_PICKER_KEY,
            SharedPreferencesDefaultValues.SKIP_FILE_PICKER_DEFAULT
        )
        Log.d("SettingsViewModel] initPreferences] skipFilePicker", spSkipFilePicker.toString())


        val spSkipFileDetails = sharedPreferences.getBoolean(
            SharedPreferenceKeys.SKIP_FILE_DETAILS_KEY,
            SharedPreferencesDefaultValues.SKIP_FILE_DETAILS_DEFAULT
        )
        Log.d("SettingsViewModel] initPreferences] skipFileDetails", spSkipFileDetails.toString())

        val spShowFilePreview = sharedPreferences.getBoolean(
            SharedPreferenceKeys.SHOW_FILE_PREVIEW_KEY,
            SharedPreferencesDefaultValues.SHOW_FILE_PREVIEW_DEFAULT
        )
        Log.d(
            "SettingsViewModel] initPreferences] showFilePreview", spShowFilePreview.toString()
        )

        val spInterceptActionViewIntents = sharedPreferences.getBoolean(
            SharedPreferenceKeys.INTERCEPT_ACTION_VIEW_INTENTS_KEY,
            SharedPreferencesDefaultValues.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT
        )
        Log.d(
            "SettingsViewModel] initPreferences] interceptActionViewIntents",
            spInterceptActionViewIntents.toString()
        )

        _defaultSaveLocation.value = spDefaultSaveLocation
        _skipFilePicker.value = spSkipFilePicker
        _skipFileDetails.value = spSkipFileDetails
        _showFilePreview.value = spShowFilePreview
        _interceptActionViewIntents.value = spInterceptActionViewIntents
    }

    fun updateDefaultSaveLocation(value: Uri?) {
        val currentSaveLocation =
            sharedPreferences.getString(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, null).let {
                try {
                    Uri.parse(it)
                } catch (_: Exception) {
                    null
                }
            }

        sharedPreferences.edit {
            if (value != null) putString(
                SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, value.toString()
            )
            else remove(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY)
        }

        if (currentSaveLocation != value) {
            currentSaveLocation?.let { saveLocationUri ->
                contentResolver.persistedUriPermissions.find { it.uri == saveLocationUri }
                    ?.let { permission ->
                        val flags =
                            (if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION
                            else 0) or (if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            else 0)

                        contentResolver.releasePersistableUriPermission(
                            currentSaveLocation, flags
                        )

                    }
            }

            value?.let { newUri ->
                contentResolver.takePersistableUriPermission(
                    newUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

            _defaultSaveLocation.value = value
        }
    }

    fun clearDefaultSaveLocation() {
        updateDefaultSaveLocation(null)
    }

    fun updateSkipFileDetails(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(SharedPreferenceKeys.SKIP_FILE_DETAILS_KEY, value)
        }

        _skipFileDetails.value = value
    }

    fun updateInterceptActionViewIntents(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(SharedPreferenceKeys.INTERCEPT_ACTION_VIEW_INTENTS_KEY, value)
            Log.d("SettingsViewModel] updateInterceptActionViewIntents", value.toString())

            try {
                val component = ComponentName(
                    packageName,
                    "com.mateusrodcosta.apps.share2storage.DetailsActivityActionViewIntentInterceptor"
                )
                packageManager.setComponentEnabledSetting(
                    component,
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )

                _interceptActionViewIntents.value = value
            } catch (e: Exception) {
                Log.e("SettingsViewModel] updateInterceptActionViewIntents", e.message, e)
            }
        }
    }

    fun updateShowFilePreview(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(SharedPreferenceKeys.SHOW_FILE_PREVIEW_KEY, value)
            Log.d("SettingsViewModel] updateShowFilePreview", value.toString())
            _showFilePreview.value = value
        }
    }

    fun updateSkipFilePicker(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(SharedPreferenceKeys.SKIP_FILE_PICKER_KEY, value)
            Log.d("SettingsViewModel] updateSkipFilePicker", value.toString())
            _skipFilePicker.value = value
        }
    }
}