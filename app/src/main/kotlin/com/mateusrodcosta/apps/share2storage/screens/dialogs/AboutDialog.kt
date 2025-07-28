/*
 *     Copyright (C) 2024 Mateus Rodrigues Costa
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

package com.mateusrodcosta.apps.share2storage.screens.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.mateusrodcosta.apps.share2storage.BuildConfig
import com.mateusrodcosta.apps.share2storage.R
import com.mateusrodcosta.apps.share2storage.screens.shared.ListItemWithURL
import com.mateusrodcosta.apps.share2storage.ui.theme.AppTheme

@Preview(apiLevel = 34)
@Composable
fun AboutDialogContentPreview() {
    AboutDialogContent()
}

@Preview(apiLevel = 34, locale = "pt-rBR")
@Composable
fun AboutDialogContentPreviewPtBr() {
    AboutDialogContent()
}

@Composable
fun AboutDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        AboutDialogContent()
    }
}

@Composable
fun AboutDialogContent() {
    AppTheme {
        Card {
            Column {
                AppInfo()
                ListItemWithURL(
                    stringResource(R.string.about_developer),
                    linkPlacement = "Mateus Rodrigues Costa",
                    url = stringResource(R.string.github_profile_url),
                )
                ListItemWithURL(
                    stringResource(R.string.about_icon_credits),
                    linkPlacement = "@swyzera",
                    url = stringResource(R.string.swy_website_url)
                )
                ListItemWithURL(
                    stringResource(R.string.about_github),
                    linkPlacement = "%s",
                    url = stringResource(R.string.source_code_url),
                    replaceWithUrl = true
                )
            }
        }
    }
}

@Composable
fun AppInfo() {
    val version = BuildConfig.VERSION_NAME

    ListItem(leadingContent = {
        Image(
            modifier = Modifier.scale(1.5f),
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
        )
    }, headlineContent = {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.primary)
        )
    }, supportingContent = {
        Text(version, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    })
}
