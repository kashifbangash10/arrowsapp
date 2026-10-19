package com.amowpuzzle.aitricker.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.BuildConfig
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.ui.components.HeaderBar
import com.amowpuzzle.aitricker.ui.components.PrimaryPillButton
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText

/**
 * Settings. Every switch writes straight through to storage and the screen renders the
 * stored value back, so there is no second copy of a preference to fall out of step.
 * Resetting progress is the one destructive action here, so it asks first.
 */
@Composable
fun SettingsScreen(
    snapshot: GamePreferences.Snapshot,
    onSound: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onGrid: (Boolean) -> Unit,
    onBoardDark: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundTint)
            .safeDrawingPadding()
    ) {
        HeaderBar(
            title = stringResource(R.string.settings_title),
            modifier = Modifier.padding(horizontal = 12.dp),
            onBack = onBack,
            backDescription = stringResource(R.string.back)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SettingsCard {
                ToggleRow(
                    label = stringResource(R.string.settings_sound),
                    checked = snapshot.soundEnabled,
                    onChange = onSound
                )
                RowDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_music),
                    checked = snapshot.musicEnabled,
                    onChange = onMusic
                )
                RowDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_haptics),
                    checked = snapshot.hapticsEnabled,
                    onChange = onHaptics
                )
                RowDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_grid),
                    checked = snapshot.showGrid,
                    onChange = onGrid
                )
                RowDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_dark_board),
                    checked = snapshot.boardDark,
                    onChange = onBoardDark
                )
            }

            Spacer(Modifier.height(16.dp))
            SettingsCard {
                Text(
                    text = stringResource(R.string.settings_reset),
                    style = AppText.LevelLabel,
                    color = AppColors.Navy
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_reset_body),
                    style = AppText.Caption,
                    color = AppColors.SlateMuted
                )
                Spacer(Modifier.height(16.dp))
                PrimaryPillButton(
                    text = stringResource(R.string.settings_reset),
                    onClick = { confirming = true },
                    container = AppColors.Blocked,
                    contentColor = AppColors.Background,
                    height = 50.dp,
                    elevation = 0.dp,
                    style = AppText.Pill
                )
            }

            Spacer(Modifier.height(16.dp))
            SettingsCard {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = AppText.LevelLabel,
                    color = AppColors.Navy
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = AppText.Body,
                    color = AppColors.Slate
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = AppText.Caption,
                    color = AppColors.SlateMuted
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = AppColors.Background,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.settings_reset_confirm_title),
                    style = AppText.CardTitle,
                    color = AppColors.Navy
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_reset_confirm_body),
                    style = AppText.Body,
                    color = AppColors.Slate
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onReset()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm_yes),
                        style = AppText.Button,
                        color = AppColors.Blocked
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(
                        text = stringResource(R.string.settings_cancel),
                        style = AppText.Button,
                        color = AppColors.Slate
                    )
                }
            }
        )
    }
}

/** One white panel; the screen is a stack of these. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Background)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.Divider)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = AppText.LevelLabel, color = AppColors.Navy)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.Background,
                checkedTrackColor = AppColors.Accent,
                checkedBorderColor = AppColors.Accent,
                uncheckedThumbColor = AppColors.Background,
                uncheckedTrackColor = AppColors.SurfaceStrong,
                uncheckedBorderColor = AppColors.SurfaceStrong
            )
        )
    }
}
