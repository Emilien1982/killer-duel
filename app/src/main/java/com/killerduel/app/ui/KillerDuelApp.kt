package com.killerduel.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.killerduel.app.data.GameMode
import com.killerduel.app.game.GameViewModel
import com.killerduel.app.game.Screen

@Composable
fun KillerDuelApp(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen != Screen.Home) {
        if (state.screen == Screen.Settings) viewModel.closeSettings() else viewModel.back()
    }

    AnimatedContent(
        targetState = state.screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
        modifier = Modifier
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                duelStats = state.duelStats,
                hasSavedGame = state.hasSavedGame,
                onTraining = { viewModel.openLevelPicker(GameMode.TRAINING) },
                onDuel = { viewModel.openLevelPicker(GameMode.DUEL) },
                onResume = viewModel::resumeSavedGame,
                onStats = viewModel::openStats
            )

            is Screen.LevelPicker -> LevelPickerScreen(
                mode = screen.mode,
                stats = state.stats,
                generating = state.generating,
                onBack = viewModel::openHome,
                onPick = { difficulty ->
                    if (screen.mode == GameMode.DUEL) viewModel.startDuel(difficulty)
                    else viewModel.startTraining(difficulty)
                }
            )

            is Screen.Matchmaking -> MatchmakingScreen(
                difficulty = screen.difficulty,
                progress = state.matchmakingProgress,
                onCancel = viewModel::openHome
            )

            Screen.Game -> state.session?.let { session ->
                GameScreen(
                    session = session,
                    onBack = viewModel::back,
                    onCell = viewModel::selectCell,
                    onDigit = viewModel::enterDigit,
                    onErase = viewModel::erase,
                    onUndo = viewModel::undo,
                    onToggleNotes = viewModel::toggleNotes,
                    onHint = viewModel::hint,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onToggleDigitFirst = viewModel::toggleDigitFirst,
                    onSettings = viewModel::openSettings,
                    onReplay = viewModel::replay
                )
            }

            Screen.Settings -> SettingsScreen(
                settings = state.settings,
                onBack = viewModel::closeSettings,
                onChange = { updated -> viewModel.updateSettings { updated } }
            )

            Screen.Stats -> StatsScreen(
                stats = state.stats,
                duelStats = state.duelStats,
                onBack = viewModel::openHome
            )
        }
    }
}
