package com.amowpuzzle.aitricker.redesign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amowpuzzle.aitricker.ui.theme.ArrowPuzzleTheme

/** Debug source set only; no campaign navigation or progress writes. */
class RedesignLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArrowPuzzleTheme { RedesignLabScreen(onClose = { finish() }) } }
    }
}
