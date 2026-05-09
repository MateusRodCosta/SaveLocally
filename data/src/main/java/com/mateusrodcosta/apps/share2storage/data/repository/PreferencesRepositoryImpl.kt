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

package com.mateusrodcosta.apps.share2storage.data.repository

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.mateusrodcosta.apps.share2storage.data.utils.SharedPreferenceUtils
import com.mateusrodcosta.apps.share2storage.data.utils.getTypedValue
import com.mateusrodcosta.apps.share2storage.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import org.koin.core.annotation.Singleton

@Singleton
class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    private val packageManager: PackageManager = context.packageManager
    private val packageName: String = context.packageName
    private val contentResolver: ContentResolver = context.contentResolver
    private val sharedPreferences: SharedPreferences =
        SharedPreferenceUtils.getDefaultSharedPreferences(context)


    private companion object SharedPreferenceKeys {
        const val DEFAULT_SAVE_LOCATION_KEY: String = "default_save_location"
        const val SKIP_FILE_PICKER_KEY: String = "skip_file_picker"
        const val SKIP_FILE_DETAILS_KEY: String = "skip_file_details"
        const val SHOW_FILE_PREVIEW_KEY: String = "show_file_preview"
        const val INTERCEPT_ACTION_VIEW_INTENTS_KEY: String = "intercept_action_view_intents"
    }

    override val defaultSaveLocation: Flow<String?> =
        preferenceFlow(DEFAULT_SAVE_LOCATION_KEY, null)

    override val skipFileDetails: Flow<Boolean> =
        preferenceFlow(
            SKIP_FILE_DETAILS_KEY,
            PreferencesRepository.SKIP_FILE_DETAILS_DEFAULT
        )

    override val showFilePreview: Flow<Boolean> =
        preferenceFlow(
            SHOW_FILE_PREVIEW_KEY,
            PreferencesRepository.SHOW_FILE_PREVIEW_DEFAULT
        )

    override val interceptActionViewIntents: Flow<Boolean> =
        preferenceFlow(
            INTERCEPT_ACTION_VIEW_INTENTS_KEY,
            PreferencesRepository.INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT
        )

    override val skipFilePicker: Flow<Boolean> =
        preferenceFlow(
            SKIP_FILE_PICKER_KEY,
            PreferencesRepository.SKIP_FILE_PICKER_DEFAULT
        )

    private fun <T> preferenceFlow(key: String, defaultValue: T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (key == changedKey) {
                trySend(prefs.getTypedValue(key, defaultValue))
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        emit(sharedPreferences.getTypedValue(key, defaultValue))
    }

    override suspend fun setDefaultSaveLocation(saveLocation: String?) {
        val currentSaveLocationString = sharedPreferences.getString(DEFAULT_SAVE_LOCATION_KEY, null)
        val currentSaveLocation =
            currentSaveLocationString.let {
                try {
                    it?.toUri()
                } catch (_: Exception) {
                    null
                }
            }

        sharedPreferences.edit {
            if (saveLocation != null) putString(
                DEFAULT_SAVE_LOCATION_KEY, saveLocation
            )
            else remove(DEFAULT_SAVE_LOCATION_KEY)
        }

        val saveLocationUri = saveLocation.let {
            try {
                it?.toUri()
            } catch (_: Exception) {
                null
            }
        }
        if (currentSaveLocation != saveLocationUri) {
            currentSaveLocation?.let { oldUri ->
                contentResolver.persistedUriPermissions.find { it.uri == oldUri }
                    ?.let { permission ->
                        val flags =
                            (if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION
                            else 0) or (if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            else 0)

                        contentResolver.releasePersistableUriPermission(
                            oldUri, flags
                        )

                    }
            }

            saveLocationUri?.let { newUri ->
                contentResolver.takePersistableUriPermission(
                    newUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    override suspend fun setSkipFileDetails(skipDetails: Boolean) {
        sharedPreferences.edit {
            putBoolean(SKIP_FILE_DETAILS_KEY, skipDetails)
        }
    }

    override suspend fun setShowFilePreview(showPreview: Boolean) {
        sharedPreferences.edit {
            putBoolean(SHOW_FILE_PREVIEW_KEY, showPreview)
        }
    }

    override suspend fun setInterceptActionViewIntents(interceptIntents: Boolean) {
        sharedPreferences.edit {
            putBoolean(INTERCEPT_ACTION_VIEW_INTENTS_KEY, interceptIntents)

            try {
                val component = ComponentName(
                    packageName,
                    "com.mateusrodcosta.apps.share2storage.DetailsActivityActionViewIntentInterceptor"
                )
                packageManager.setComponentEnabledSetting(
                    component,
                    if (interceptIntents) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                Log.e("PreferencesRepositoryImpl", "updateInterceptActionViewIntents error", e)
            }
        }
    }

    override suspend fun setSkipFilePicker(skipPicker: Boolean) {
        sharedPreferences.edit {
            putBoolean(SKIP_FILE_PICKER_KEY, skipPicker)
        }
    }
}
