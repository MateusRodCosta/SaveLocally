/*
 *     Copyright (C) 2022 - 2024 Mateus Rodrigues Costa
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
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.mateusrodcosta.apps.share2storage.utils.SharedPreferenceKeys
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        contentResolver = context.contentResolver
        packageManager = context.packageManager
        packageName = context.packageName
    }

    fun initPreferences() {
        val spDefaultSaveLocationRaw =
            sharedPreferences.getString(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, null)
        val spDefaultSaveLocation = if (spDefaultSaveLocationRaw != null) try {
            val uri = Uri.parse(spDefaultSaveLocationRaw)

            Log.d("settings] initSharedPreferences] uri", uri.toString())
            Log.d("settings] initSharedPreferences] uri.path", uri.path.toString())

            uri
        } catch (_: Exception) {
            null
        }
        else null

        val spSkipFileDetails =
            sharedPreferences.getBoolean(SharedPreferenceKeys.SKIP_FILE_DETAILS_KEY, false)
        Log.d("settings] initSharedPreferences] skipFileDetails", spSkipFileDetails.toString())

        val spInterceptActionViewIntents = sharedPreferences.getBoolean(
            SharedPreferenceKeys.INTERCEPT_ACTION_VIEW_INTENTS_KEY, false
        )
        Log.d(
            "settings] initSharedPreferences] interceptActionViewIntents",
            spInterceptActionViewIntents.toString()
        )

        val spShowFilePreview = sharedPreferences.getBoolean(
            SharedPreferenceKeys.SHOW_FILE_PREVIEW_KEY, true
        )
        Log.d(
            "settings] initSharedPreferences] showFilePreview", spShowFilePreview.toString()
        )

        val spSkipFilePicker =
            sharedPreferences.getBoolean(SharedPreferenceKeys.SKIP_FILE_PICKER, false)
        Log.d("settings] initSharedPreferences] skipFilePicker", spSkipFilePicker.toString())

        _defaultSaveLocation.value = spDefaultSaveLocation
        _skipFileDetails.value = spSkipFileDetails
        _interceptActionViewIntents.value = spInterceptActionViewIntents
        _showFilePreview.value = spShowFilePreview
        _skipFilePicker.value = spSkipFilePicker
    }

    fun updateDefaultSaveLocation(value: Uri?) {
        val currentSaveLocationRaw =
            sharedPreferences.getString(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, null)

        sharedPreferences.edit(commit = true) {
            if (value != null) putString(
                SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY, value.toString()
            )
            else remove(SharedPreferenceKeys.DEFAULT_SAVE_LOCATION_KEY)

        }

        val currentSaveLocation: Uri? = if (currentSaveLocationRaw != null) try {
            Uri.parse(currentSaveLocationRaw)
        } catch (_: Exception) {
            null
        }
        else null

        if (currentSaveLocation != null) {
            contentResolver.persistedUriPermissions.forEach {
                if (it.uri == currentSaveLocation) {
                    val isRead = if (it.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION
                    else 0
                    val isWrite = if (it.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    else 0

                    contentResolver.releasePersistableUriPermission(
                        currentSaveLocation, isRead or isWrite
                    )
                }
            }
        }

        if (value != null) contentResolver.takePersistableUriPermission(
            value, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        _defaultSaveLocation.value = value
    }

    fun clearSaveDirectory() {
        updateDefaultSaveLocation(null)
    }

    fun updateSkipFileDetails(value: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(SharedPreferenceKeys.SKIP_FILE_DETAILS_KEY, value)
        }

        _skipFileDetails.value = value
    }

    fun updateInterceptActionViewIntents(value: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(SharedPreferenceKeys.INTERCEPT_ACTION_VIEW_INTENTS_KEY, value)
            Log.e("settings] updateInterceptActionViewIntents", value.toString())

            try {
                val component = ComponentName(
                    packageName,
                    "com.mateusrodcosta.apps.share2storage.DetailsActivityActionViewIntentInterceptor"
                )
                packageManager.setComponentEnabledSetting(
                    component,
                    if (value) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED,
                    DONT_KILL_APP
                )

                _interceptActionViewIntents.value = value
            } catch (e: Exception) {
                Log.e("settings] updateInterceptActionViewIntents", e.toString())
            }
        }
    }

    fun updateShowFilePreview(value: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(SharedPreferenceKeys.SHOW_FILE_PREVIEW_KEY, value)
            Log.e("settings] updateShowFilePreview", value.toString())
            _showFilePreview.value = value
        }
    }

    fun updateSkipFilePicker(value: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(SharedPreferenceKeys.SKIP_FILE_PICKER, value)
            Log.e("settings] updateSkipFilePicker", value.toString())
            _skipFilePicker.value = value
        }
    }
}