package com.amowpuzzle.aitricker.presentation.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amowpuzzle.aitricker.AppContainer
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.presentation.complete.LevelCompleteScreen
import com.amowpuzzle.aitricker.presentation.game.GameScreen
import com.amowpuzzle.aitricker.presentation.game.GameViewModel
import com.amowpuzzle.aitricker.presentation.levels.LevelSelectScreen
import com.amowpuzzle.aitricker.presentation.menu.MainMenuScreen
import com.amowpuzzle.aitricker.presentation.settings.SettingsScreen
import com.amowpuzzle.aitricker.presentation.splash.SplashScreen
import com.amowpuzzle.aitricker.presentation.tutorial.TutorialScreen
import com.amowpuzzle.aitricker.ui.components.SystemBarAppearance
import com.amowpuzzle.aitricker.ui.theme.Motion
import kotlinx.coroutines.launch

/** Every destination in the app, and the builders for the ones that take arguments. */
object Routes {
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val LEVELS = "levels"
    const val SETTINGS = "settings"
    const val TUTORIAL = "tutorial"
    const val GAME = "game/{levelId}"
    const val COMPLETE = "complete/{levelId}/{moves}/{lives}"

    fun game(levelId: Int) = "game/$levelId"

    fun complete(levelId: Int, moves: Int, lives: Int) = "complete/$levelId/$moves/$lives"
}

/**
 * The whole navigation graph.
 *
 * Finishing a level replaces the game destination rather than stacking on top of it,
 * and the completion screen is replaced in turn when the player moves on, so the back
 * stack stays at most two deep however many levels are played in a row - and Back from
 * a finished level never lands on a board that has already been cleared.
 */
@Composable
fun AppNavigation(
    container: AppContainer,
    snapshot: GamePreferences.Snapshot,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val preferences = container.preferences
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    SystemBarAppearance(currentRoute == Routes.COMPLETE || currentRoute == Routes.LEVELS ||
        (currentRoute == Routes.GAME && snapshot.boardDark &&
            currentEntry?.arguments?.getInt("levelId") != 1))

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        // The requested fade, with a little depth in it: a screen arriving eases up from
        // slightly small, and the one leaving eases slightly large as it dims, so forward
        // and back read as moving through the app rather than as a plain crossfade.
        enterTransition = {
            fadeIn(tween(Motion.screenEnterMs, easing = LinearOutSlowInEasing)) +
                scaleIn(
                    initialScale = Motion.ENTER_SCALE,
                    animationSpec = tween(Motion.screenEnterMs, easing = LinearOutSlowInEasing)
                )
        },
        exitTransition = {
            fadeOut(tween(Motion.screenExitMs, easing = FastOutLinearInEasing)) +
                scaleOut(
                    targetScale = Motion.EXIT_SCALE,
                    animationSpec = tween(Motion.screenExitMs, easing = FastOutLinearInEasing)
                )
        },
        popEnterTransition = {
            fadeIn(tween(Motion.screenEnterMs, easing = LinearOutSlowInEasing)) +
                scaleIn(
                    initialScale = Motion.POP_ENTER_SCALE,
                    animationSpec = tween(Motion.screenEnterMs, easing = LinearOutSlowInEasing)
                )
        },
        popExitTransition = {
            fadeOut(tween(Motion.screenExitMs, easing = FastOutLinearInEasing)) +
                scaleOut(
                    targetScale = Motion.ENTER_SCALE,
                    animationSpec = tween(Motion.screenExitMs, easing = FastOutLinearInEasing)
                )
        }
    ) {
        // The opening animation. It replaces itself with whatever the caller decided the
        // real first destination is, so Back from Home leaves the app rather than
        // replaying the intro.
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(startDestination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainMenuScreen(
                snapshot = snapshot,
                onPlay = { navController.navigate(Routes.game(it)) },
                onLevels = { navController.navigate(Routes.LEVELS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onTutorial = { navController.navigate(Routes.TUTORIAL) }
            )
        }

        composable(Routes.LEVELS) {
            LevelSelectScreen(
                snapshot = snapshot,
                onLevel = { navController.navigate(Routes.game(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                snapshot = snapshot,
                onSound = { scope.launch { preferences.setSoundEnabled(it) } },
                onMusic = { scope.launch { preferences.setMusicEnabled(it) } },
                onHaptics = { scope.launch { preferences.setHapticsEnabled(it) } },
                onGrid = { scope.launch { preferences.setShowGrid(it) } },
                onBoardDark = { scope.launch { preferences.setBoardDark(it) } },
                onReset = { scope.launch { preferences.resetProgress() } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TUTORIAL) {
            LaunchedEffect(Unit) {
                navController.navigate(Routes.game(1)) {
                    popUpTo(Routes.TUTORIAL) { inclusive = true }
                }
            }
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { entry ->
            val levelId = entry.arguments?.getInt("levelId") ?: 1
            GameRoute(
                container = container,
                levelId = levelId,
                showGrid = snapshot.showGrid,
                boardDark = snapshot.boardDark,
                navController = navController
            )
        }

        composable(
            route = Routes.COMPLETE,
            arguments = listOf(
                navArgument("levelId") { type = NavType.IntType },
                navArgument("moves") { type = NavType.IntType },
                navArgument("lives") { type = NavType.IntType }
            )
        ) { entry ->
            val args = entry.arguments
            val levelId = args?.getInt("levelId") ?: 1
            val next = (levelId + 1).coerceAtMost(LevelCatalog.TOTAL_LEVELS)
            LevelCompleteScreen(
                levelId = levelId,
                moves = args?.getInt("moves") ?: 0,
                livesLeft = args?.getInt("lives") ?: 0,
                onNext = {
                    navController.navigate(Routes.game(next)) {
                        popUpTo(Routes.COMPLETE) { inclusive = true }
                    }
                },
                onReplay = {
                    navController.navigate(Routes.game(levelId)) {
                        popUpTo(Routes.COMPLETE) { inclusive = true }
                    }
                },
                onMenu = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Hosts one play session. The view model is scoped to this destination, so leaving the
 * level disposes it and returning starts a clean session; [GameViewModel.load] is keyed
 * on the level so it runs once, not on every recomposition.
 */
@Composable
private fun GameRoute(
    container: AppContainer,
    levelId: Int,
    showGrid: Boolean,
    boardDark: Boolean,
    navController: NavHostController
) {
    val gameViewModel: GameViewModel = viewModel(
        factory = GameViewModel.factory(
            preferences = container.preferences,
            sound = container.sound,
            haptics = container.haptics
        )
    )
    val state by gameViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(levelId) { gameViewModel.load(levelId) }

    GameScreen(
        state = state,
        showGrid = showGrid,
        boardDark = boardDark,
        onTap = gameViewModel::tap,
        onDepartureFinished = gameViewModel::onDepartureFinished,
        onHint = gameViewModel::useHint,
        onToggleGrid = { gameViewModel.toggleGrid(showGrid) },
        onRetry = gameViewModel::retry,
        onBack = { navController.popBackStack() },
        onSettings = { navController.navigate(Routes.SETTINGS) },
        onSolved = { id, moves, lives ->
            navController.navigate(Routes.complete(id, moves, lives)) {
                popUpTo(Routes.GAME) { inclusive = true }
            }
        },
        onPlaySolution = gameViewModel::playSolution
    )
}
