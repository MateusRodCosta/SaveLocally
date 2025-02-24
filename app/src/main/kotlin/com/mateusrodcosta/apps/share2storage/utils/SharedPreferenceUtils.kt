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

package com.mateusrodcosta.apps.share2storage.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPreferenceKeys {
    const val DEFAULT_SAVE_LOCATION_KEY: String = "default_save_location"
    const val SKIP_FILE_PICKER_KEY: String = "skip_file_picker"
    const val SKIP_FILE_DETAILS_KEY: String = "skip_file_details"
    const val SHOW_FILE_PREVIEW_KEY: String = "show_file_preview"
    const val INTERCEPT_ACTION_VIEW_INTENTS_KEY: String = "intercept_action_view_intents"
}

object SharedPreferencesDefaultValues {
    const val SKIP_FILE_PICKER_DEFAULT: Boolean = false
    const val SKIP_FILE_DETAILS_DEFAULT: Boolean = true
    const val SHOW_FILE_PREVIEW_DEFAULT: Boolean = false
    const val INTERCEPT_ACTION_VIEW_INTENTS_DEFAULT: Boolean = false
}

// Sourced from https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:preference/preference/src/main/java/androidx/preference/PreferenceManager.java
// and converted to Kotlin
// Instead of relying on androidx's https://developer.android.com/jetpack/androidx/releases/preference
class SharedPreferenceUtils {
    companion object {
        /**
         * Gets a {@link SharedPreferences} instance that points to the default file that is used by
         * the preference framework in the given context.
         *
         * @param context The context of the preferences whose values are wanted
         * @return A {@link SharedPreferences} instance that can be used to retrieve and listen to
         * values of the preferences
         */
        @JvmStatic
        fun getDefaultSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                getDefaultSharedPreferencesName(context), getDefaultSharedPreferencesMode()
            )
        }

        @JvmStatic
        private fun getDefaultSharedPreferencesName(context: Context): String {
            return context.packageName + "_preferences"
        }

        @JvmStatic
        private fun getDefaultSharedPreferencesMode(): Int {
            return Context.MODE_PRIVATE
        }
    }
}

