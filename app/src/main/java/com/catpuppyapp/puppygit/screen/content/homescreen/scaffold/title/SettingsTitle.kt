package com.catpuppyapp.puppygit.screen.content.homescreen.scaffold.title

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.catpuppyapp.puppygit.play.pro.R

@Composable
fun SettingsTitle() {
    Text(
        text = stringResource(id = R.string.settings),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}