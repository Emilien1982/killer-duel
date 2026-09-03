package com.killerduel.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.PuzzleGenerator
import com.killerduel.app.data.GameMode
import com.killerduel.app.data.GameRepository
import com.killerduel.app.data.RecordedSession
import com.killerduel.app.data.SavedGame
import com.killerduel.app.opponent.OpponentPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var lastTickAt = 0L

    init {
        viewModelScope.launch {
            combine(repository.stats, repository.duelStats) { stats, duels -> stats to duels }
                .collect { (stats, duels) ->
                    _state.value = _state.value.copy(stats = stats, duelStats = duels)
                }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(hasSavedGame = repository.loadInProgress() != null)
        }
    }

    // ---- Navigation ----

    fun openHome() {
        stopTicker()
        _state.value = _state.value.copy(screen = Screen.Home)
    }

    fun openLevelPicker(mode: GameMode) {
        _state.value = _state.value.copy(screen = Screen.LevelPicker(mode))
    }

    fun openStats() {
        _state.value = _state.value.copy(screen = Screen.Stats)
    }

    fun back() {
        val current = _state.value
        when (current.screen) {
            is Screen.Game -> {
                val session = current.session
                stopTicker()
                if (session != null && session.mode == GameMode.DUEL && !session.finished) {
                    // Quitter un duel, c'est déclarer forfait : l'adversaire, lui, continue.
                    viewModelScope.launch { repository.recordDuel(won = false) }
                    _state.value = current.copy(screen = Screen.Home, session = null)
                } else {
                    saveProgress()
                    _state.value = current.copy(
                        screen = Screen.Home,
                        hasSavedGame = session != null && !session.finished
                    )
                }
            }
            else -> openHome()
        }
    }

    // ---- Démarrage d'une partie ----

    fun startTraining(difficulty: Difficulty) {
        viewModelScope.launch {
            _state.value = _state.value.copy(generating = true)
            val puzzle = withContext(Dispatchers.Default) { PuzzleGenerator.generate(difficulty) }
            repository.recordGameStarted(difficulty)
            repository.saveInProgress(null)
            _state.value = _state.value.copy(
                screen = Screen.Game,
                generating = false,
                hasSavedGame = false,
                session = GameSession(puzzle = puzzle, mode = GameMode.TRAINING)
            )
            startTicker()
        }
    }

    /**
     * Le duel affiche d'abord une recherche d'adversaire : le temps que la grille
     * se génère, on met en scène l'appariement plutôt qu'un écran de chargement.
     */
    fun startDuel(difficulty: Difficulty) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                screen = Screen.Matchmaking(difficulty),
                matchmakingProgress = 0f
            )

            val puzzleDeferred = withContext(Dispatchers.Default) {
                PuzzleGenerator.generate(difficulty)
            }
            val seed = Random.nextLong()
            val engine = OpponentPicker(repository).pick(difficulty, seed)
            val plan = withContext(Dispatchers.Default) { engine.plan(puzzleDeferred, seed) }

            val steps = 20
            repeat(steps) { i ->
                delay(MATCHMAKING_MILLIS / steps)
                _state.value = _state.value.copy(matchmakingProgress = (i + 1f) / steps)
            }

            // Les statistiques par niveau ne comptent que l'entraînement ;
            // les duels ont leur propre bilan.
            repository.saveInProgress(null)
            _state.value = _state.value.copy(
                screen = Screen.Game,
                hasSavedGame = false,
                session = GameSession(
                    puzzle = puzzleDeferred,
                    mode = GameMode.DUEL,
                    opponent = plan
                )
            )
            startTicker()
        }
    }

    /** Relance une partie identique en mode et en niveau. */
    fun replay() {
        val session = _state.value.session ?: return
        val difficulty = session.puzzle.difficulty
        if (session.mode == GameMode.DUEL) startDuel(difficulty) else startTraining(difficulty)
    }

    fun resumeSavedGame() {
        viewModelScope.launch {
            val saved = repository.loadInProgress() ?: return@launch
            _state.value = _state.value.copy(
                screen = Screen.Game,
                session = GameSession(
                    puzzle = saved.puzzle,
                    mode = saved.mode,
                    entries = saved.entries,
                    notes = saved.notes,
                    mistakes = saved.mistakes,
                    elapsedMillis = saved.elapsedMillis,
                    moveLog = saved.moveLog,
                    wrongCells = saved.entries.indices
                        .filter { saved.entries[it] != 0 && saved.entries[it] != saved.puzzle.solution[it] }
                        .toSet()
                )
            )
            startTicker()
        }
    }

    // ---- Actions de jeu ----

    fun selectCell(cell: Int) = mutate { copy(selected = cell) }

    fun enterDigit(digit: Int) = mutate { withDigit(digit) }

    fun erase() = mutate { withErase() }

    fun undo() = mutate { withUndo() }

    fun toggleNotes() = mutate { copy(pencil = !pencil) }

    fun hint() = mutate { withHint() }

    fun pause() = mutate { copy(paused = true) }

    fun resume() = mutate { copy(paused = false) }

    private inline fun mutate(block: GameSession.() -> GameSession) {
        val current = _state.value.session ?: return
        val wasFinished = current.finished
        val updated = current.block()
        _state.value = _state.value.copy(session = updated)
        if (!wasFinished && updated.finished) onGameFinished(updated)
    }

    // ---- Horloge ----

    private fun startTicker() {
        stopTicker()
        lastTickAt = System.currentTimeMillis()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                val now = System.currentTimeMillis()
                val delta = now - lastTickAt
                lastTickAt = now

                val session = _state.value.session ?: continue
                if (session.paused || session.finished) continue

                val advanced = session
                    .copy(elapsedMillis = session.elapsedMillis + delta)
                    .withOpponentCheck()
                _state.value = _state.value.copy(session = advanced)
                if (advanced.finished) onGameFinished(advanced)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    // ---- Fin de partie ----

    private fun onGameFinished(session: GameSession) {
        stopTicker()
        viewModelScope.launch {
            repository.saveInProgress(null)
            if (session.mode == GameMode.DUEL) {
                repository.recordDuel(session.outcome == Outcome.WON)
            }
            // Seules les parties d'entraînement menées à leur terme alimentent
            // l'historique : ce sont elles qui fourniront les futurs adversaires.
            if (session.mode == GameMode.TRAINING && session.outcome == Outcome.WON) {
                repository.recordTrainingSession(
                    RecordedSession(
                        id = UUID.randomUUID().toString(),
                        difficulty = session.puzzle.difficulty,
                        puzzleSeed = session.puzzle.seed,
                        completedAtEpochMillis = System.currentTimeMillis(),
                        durationMillis = session.elapsedMillis,
                        mistakes = session.mistakes,
                        moves = session.moveLog
                    )
                )
            }
            _state.value = _state.value.copy(hasSavedGame = false)
        }
    }

    fun saveProgress() {
        val session = _state.value.session ?: return
        // Un duel ne se met pas en pause : seul l'entraînement est repris plus tard.
        if (session.finished || session.mode != GameMode.TRAINING) return
        viewModelScope.launch {
            repository.saveInProgress(
                SavedGame(
                    puzzle = session.puzzle,
                    mode = session.mode,
                    entries = session.entries,
                    notes = session.notes,
                    mistakes = session.mistakes,
                    elapsedMillis = session.elapsedMillis,
                    moveLog = session.moveLog
                )
            )
        }
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    companion object {
        private const val TICK_MILLIS = 250L
        private const val MATCHMAKING_MILLIS = 2_400L

        fun factory(repository: GameRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(repository) as T
        }
    }
}
