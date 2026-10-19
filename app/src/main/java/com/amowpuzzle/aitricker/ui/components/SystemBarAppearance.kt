package com.amowpuzzle.aitricker.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** The navigation owner selects icon contrast once, including during overlapping fades. */
@Composable
internal fun SystemBarAppearance(darkSurface: Boolean) {
    val activity = LocalContext.current.findActivity()
    val view = LocalView.current
    SideEffect {
        if (activity != null && !view.isInEditMode) {
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkSurface
                isAppearanceLightNavigationBars = !darkSurface
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeIf { it !== this }?.findActivity()
    else -> null
}
