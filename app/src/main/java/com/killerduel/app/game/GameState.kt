package com.killerduel.app.game

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.Puzzle
import com.killerduel.app.core.bit
import com.killerduel.app.core.maskContains
import com.killerduel.app.data.DifficultyStats
import com.killerduel.app.data.GameSettings
import com.killerduel.app.data.DuelStats
import com.killerduel.app.data.GameMode
import com.killerduel.app.data.RecordedMove
import com.killerduel.app.opponent.OpponentPlan

/** Écrans de l'application. La navigation tient dans l'état, sans graphe séparé. */
sealed interface Screen {
    data object Home : Screen
    data class LevelPicker(val mode: GameMode) : Screen
    data class Matchmaking(val difficulty: Difficulty) : Screen
    data object Game : Screen
    data object Stats : Screen
    data object Settings : Screen
}

/** Issue d'une partie. */
enum class Outcome { WON, LOST_ON_TIME, LOST_ON_MISTAKES }

/** Instantané minimal pour l'annulation. */
data class Snapshot(
    val entries: List<Int>,
    val notes: List<Int>,
    val mistakes: Int,
    val wrongCells: Set<Int>
)

data class GameSession(
    val puzzle: Puzzle,
    val mode: GameMode,
    val entries: List<Int> = List(81) { 0 },
    val notes: List<Int> = List(81) { 0 },
    val selected: Int = -1,
    val pencil: Boolean = false,
    /** Réglages en vigueur pour cette partie. */
    val settings: GameSettings = GameSettings(),
    /** Chiffre armé en mode « chiffre d'abord », 0 si aucun. */
    val activeDigit: Int = 0,
    val mistakes: Int = 0,
    val hintsLeft: Int = MAX_HINTS,
    val wrongCells: Set<Int> = emptySet(),
    val elapsedMillis: Long = 0,
    val paused: Boolean = false,
    val history: List<Snapshot> = emptyList(),
    val moveLog: List<RecordedMove> = emptyList(),
    val opponent: OpponentPlan? = null,
    /** Date du défi du jour quand la partie en est un, sinon null. */
    val dailyDate: String? = null,
    val outcome: Outcome? = null
) {
    val finished: Boolean get() = outcome != null

    /** Cases correctement remplies, chiffres donnés compris. */
    val filledCount: Int
        get() = (0 until 81).count { puzzle.givens[it] != 0 || entries[it] == puzzle.solution[it] }

    val solved: Boolean get() = filledCount == 81

    val opponentFilled: Int
        get() = opponent?.let { it.filledAt(elapsedMillis) + puzzle.givens.count { g -> g != 0 } } ?: 0

    /**
     * Combien d'exemplaires d'un chiffre restent à placer. Les chiffres faux ne
     * comptent pas : ils condamneraient une touche encore utile.
     */
    fun remainingCount(digit: Int): Int =
        9 - (0 until 81).count { valueAt(it) == digit && puzzle.solution[it] == digit }

    fun isDigitExhausted(digit: Int): Boolean = remainingCount(digit) <= 0

    fun valueAt(cell: Int): Int =
        if (puzzle.givens[cell] != 0) puzzle.givens[cell] else entries[cell]

    /**
     * Une case juste est acquise : ni le joueur ni un chiffre armé ne peuvent
     * la reprendre. Seule l'annulation revient en arrière.
     */
    fun isLocked(cell: Int): Boolean =
        puzzle.givens[cell] != 0 || entries[cell] == puzzle.solution[cell]

    fun snapshot() = Snapshot(entries, notes, mistakes, wrongCells)

    companion object {
        const val MAX_MISTAKES = 3
        const val MAX_HINTS = 3
    }
}

data class AppState(
    val screen: Screen = Screen.Home,
    val session: GameSession? = null,
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
    val duelStats: DuelStats = DuelStats(),
    val hasSavedGame: Boolean = false,
    val settings: GameSettings = GameSettings(),
    val dailyWins: Set<String> = emptySet(),
    val generating: Boolean = false,
    val matchmakingProgress: Float = 0f
)

// ---- Transformations pures, testables sans Android ----

/** Pose un chiffre (ou une note) dans la case sélectionnée. */
fun GameSession.withDigit(digit: Int): GameSession {
    val cell = selected
    if (cell !in 0..80 || finished || paused) return this
    if (isLocked(cell)) return this

    if (pencil) {
        val updated = notes.toMutableList()
        updated[cell] = updated[cell] xor bit(digit)
        return copy(notes = updated, history = history + snapshot())
    }

    // Reposer le même chiffre l'efface, comme dans les jeux du genre.
    if (entries[cell] == digit) return withErase()

    val updatedEntries = entries.toMutableList()
    updatedEntries[cell] = digit
    val updatedNotes = notes.toMutableList()
    updatedNotes[cell] = 0

    val correct = digit == puzzle.solution[cell]
    val updatedWrong = if (correct) wrongCells - cell else wrongCells + cell
    val updatedMistakes = if (correct) mistakes else mistakes + 1

    // Placer un chiffre juste retire la note correspondante chez les voisines.
    if (correct && settings.autoClearNotes) {
        for (peer in com.killerduel.app.core.PEERS[cell]) {
            if (maskContains(updatedNotes[peer], digit)) {
                updatedNotes[peer] = updatedNotes[peer] xor bit(digit)
            }
        }
    }

    val log = if (correct) moveLog + RecordedMove(elapsedMillis, cell, digit) else moveLog

    return copy(
        entries = updatedEntries,
        notes = updatedNotes,
        mistakes = updatedMistakes,
        wrongCells = updatedWrong,
        moveLog = log,
        history = history + snapshot()
    ).withCompletionCheck()
}

fun GameSession.withErase(): GameSession {
    val cell = selected
    if (cell !in 0..80 || finished || paused) return this
    if (isLocked(cell)) return this
    if (entries[cell] == 0 && notes[cell] == 0) return this

    val updatedEntries = entries.toMutableList().also { it[cell] = 0 }
    val updatedNotes = notes.toMutableList().also { it[cell] = 0 }
    return copy(
        entries = updatedEntries,
        notes = updatedNotes,
        wrongCells = wrongCells - cell,
        history = history + snapshot()
    )
}

fun GameSession.withUndo(): GameSession {
    if (finished || paused) return this
    val previous = history.lastOrNull() ?: return this
    return copy(
        entries = previous.entries,
        notes = previous.notes,
        mistakes = previous.mistakes,
        wrongCells = previous.wrongCells,
        history = history.dropLast(1)
    )
}

/** Révèle la case sélectionnée, ou la première case vide si aucune n'est choisie. */
fun GameSession.withHint(): GameSession {
    if (hintsLeft <= 0 || finished || paused) return this
    val cell = selected.takeIf { it in 0..80 && valueAt(it) != puzzle.solution[it] }
        ?: (0 until 81).firstOrNull { puzzle.givens[it] == 0 && entries[it] != puzzle.solution[it] }
        ?: return this

    val digit = puzzle.solution[cell]
    val updatedEntries = entries.toMutableList().also { it[cell] = digit }
    val updatedNotes = notes.toMutableList().also { it[cell] = 0 }

    return copy(
        entries = updatedEntries,
        notes = updatedNotes,
        wrongCells = wrongCells - cell,
        hintsLeft = hintsLeft - 1,
        selected = cell,
        moveLog = moveLog + RecordedMove(elapsedMillis, cell, digit),
        history = history + snapshot()
    ).withCompletionCheck()
}

/**
 * Appui sur une touche du pavé. En mode « chiffre d'abord » la touche arme le
 * chiffre au lieu de l'écrire ; la réappuyer le désarme, et en changer ne
 * demande pas de toucher à l'interrupteur.
 */
fun GameSession.withKeyPress(digit: Int): GameSession {
    if (finished || paused) return this
    if (!settings.digitFirst) return withDigit(digit)
    return copy(activeDigit = if (activeDigit == digit) 0 else digit)
}

/**
 * Appui sur une case. Elle est toujours sélectionnée ; en mode « chiffre
 * d'abord » avec un chiffre armé, elle est aussi remplie dans la foulée.
 */
fun GameSession.withCellPress(cell: Int): GameSession {
    if (cell !in 0..80) return this
    val selectedSession = copy(selected = cell)
    if (!settings.digitFirst || activeDigit == 0 || finished || paused) return selectedSession
    return selectedSession.withDigit(activeDigit)
}

/** Applique de nouveaux réglages ; un changement de mode désarme le chiffre. */
fun GameSession.withSettings(updated: GameSettings): GameSession =
    copy(
        settings = updated,
        activeDigit = if (updated.digitFirst) activeDigit else 0
    ).let { if (updated.mistakesLimit) it.withCompletionCheck() else it }

/** Applique les fins de partie : grille terminée ou trop d'erreurs. */
fun GameSession.withCompletionCheck(): GameSession = when {
    solved -> copy(outcome = Outcome.WON, selected = -1)
    settings.mistakesLimit && mistakes >= GameSession.MAX_MISTAKES ->
        copy(outcome = Outcome.LOST_ON_MISTAKES)
    else -> this
}

/** Vérifie si l'adversaire vient de terminer avant le joueur. */
fun GameSession.withOpponentCheck(): GameSession {
    val plan = opponent ?: return this
    if (finished) return this
    if (plan.filledAt(elapsedMillis) >= plan.moves.size && plan.moves.isNotEmpty()) {
        return copy(outcome = Outcome.LOST_ON_TIME)
    }
    return this
}
