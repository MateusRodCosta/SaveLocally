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

package com.mateusrodcosta.apps.share2storage.data.utils

import android.content.Context
import android.content.SharedPreferences

@Suppress("UNCHECKED_CAST")
fun <T> SharedPreferences.getTypedValue(key: String, defaultValue: T): T {
    return when (defaultValue) {
        is String? -> getString(key, defaultValue) as T
        is Boolean -> getBoolean(key, defaultValue) as T
        is Int -> getInt(key, defaultValue) as T
        is Long -> getLong(key, defaultValue) as T
        else -> throw IllegalArgumentException("Unsupported type")
    }
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

