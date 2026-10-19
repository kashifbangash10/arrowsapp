package com.amowpuzzle.aitricker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.presentation.navigation.AppNavigation
import com.amowpuzzle.aitricker.presentation.navigation.Routes
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.ArrowPuzzleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The only Activity: it hosts the Compose graph and nothing else.
 *
 * The background drone is driven from here rather than from a screen, because it
 * belongs to the app being on screen, not to any one destination. Tying it to the
 * STARTED state means it stops when the player leaves and comes back when they
 * return, and doing the work inside a single coroutine keeps the start/stop pair
 * ordered - the pad is rendered on a background thread, so a quick switch away
 * can never leave a half-built loop playing to an empty room.
 */
class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        container = appContainer

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    container.preferences.snapshots
                        .map { it.musicEnabled }
                        .distinctUntilChanged()
                        .collect { wanted ->
                            // Rendering two seconds of audio is real work; never on the UI thread.
                            withContext(Dispatchers.Default) { container.music.setEnabled(wanted) }
                        }
                } finally {
                    container.music.stop()
                }
            }
        }

        setContent {
            ArrowPuzzleTheme {
                GameRoot(container = container, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Waits for stored settings to arrive, then hands them to the graph.
 *
 * Nothing is drawn until the first snapshot exists, which is a single frame in
 * practice and avoids the flicker of a screen rendered with default settings and
 * then corrected. The start destination is decided once and remembered, so
 * clearing progress from Settings cannot pull the graph out from under the player
 * mid-session.
 */
@Composable
private fun GameRoot(container: AppContainer, modifier: Modifier = Modifier) {
    val snapshots: Flow<GamePreferences.Snapshot?> = container.preferences.snapshots
    val state = snapshots.collectAsStateWithLifecycle(initialValue = null)
    val snapshot = state.value

    Box(modifier = modifier.background(AppColors.BackgroundTint)) {
        if (snapshot != null) {
            val start = remember {
                if (snapshot.completed.isEmpty()) Routes.game(1) else Routes.MAIN
            }

            LaunchedEffect(snapshot.soundEnabled, snapshot.hapticsEnabled) {
                container.sound.enabled = snapshot.soundEnabled
                container.haptics.enabled = snapshot.hapticsEnabled
            }

            AppNavigation(
                container = container,
                snapshot = snapshot,
                startDestination = start,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
