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

package com.mateusrodcosta.apps.share2storage

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.mateusrodcosta.apps.share2storage.di.AppModule
import com.mateusrodcosta.apps.share2storage.utils.ThumbnailFetcher
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class SaveLocallyApp: Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()

        startKoin<SaveLocallyApp> {
            androidLogger()
            androidContext(this@SaveLocallyApp)
        }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(ThumbnailFetcher.Factory(context))
            }
            .build()
    }
}