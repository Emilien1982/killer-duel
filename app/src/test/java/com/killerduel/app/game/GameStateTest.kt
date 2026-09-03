package com.killerduel.app.game

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.PuzzleGenerator
import com.killerduel.app.core.bit
import com.killerduel.app.core.maskContains
import com.killerduel.app.data.GameMode
import com.killerduel.app.opponent.SyntheticOpponentEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    private val puzzle = PuzzleGenerator.generate(Difficulty.EASY, seed = 314L)
    private val firstEmpty = puzzle.givens.indexOfFirst { it == 0 }
    private val base = GameSession(puzzle = puzzle, mode = GameMode.TRAINING)

    private fun sessionOn(cell: Int) = base.copy(selected = cell)

    @Test
    fun `placing the right digit records a move and no mistake`() {
        val correct = puzzle.solution[firstEmpty]
        val after = sessionOn(firstEmpty).withDigit(correct)

        assertEquals(correct, after.entries[firstEmpty])
        assertEquals(0, after.mistakes)
        assertTrue(after.wrongCells.isEmpty())
        assertEquals(1, after.moveLog.size)
        assertEquals(firstEmpty, after.moveLog.first().cell)
    }

    @Test
    fun `placing a wrong digit counts a mistake and is not recorded`() {
        val wrong = (1..9).first { it != puzzle.solution[firstEmpty] }
        val after = sessionOn(firstEmpty).withDigit(wrong)

        assertEquals(1, after.mistakes)
        assertTrue(firstEmpty in after.wrongCells)
        assertTrue("un coup faux ne doit pas nourrir l'historique", after.moveLog.isEmpty())
    }

    @Test
    fun `three mistakes end the game`() {
        var session = base
        val cells = puzzle.givens.indices.filter { puzzle.givens[it] == 0 }.take(3)
        cells.forEach { cell ->
            val wrong = (1..9).first { it != puzzle.solution[cell] }
            session = session.copy(selected = cell).withDigit(wrong)
        }
        assertEquals(Outcome.LOST_ON_MISTAKES, session.outcome)
    }

    @Test
    fun `given cells are read only`() {
        val givenCell = puzzle.givens.indexOfFirst { it != 0 }
        val after = sessionOn(givenCell).withDigit(5)
        assertEquals(base.entries, after.entries)
        assertEquals(0, after.mistakes)
    }

    @Test
    fun `pencil mode toggles notes instead of values`() {
        val session = sessionOn(firstEmpty).copy(pencil = true)
        val withNote = session.withDigit(4)
        assertTrue(maskContains(withNote.notes[firstEmpty], 4))
        assertEquals(0, withNote.entries[firstEmpty])

        val removed = withNote.copy(selected = firstEmpty).withDigit(4)
        assertFalse(maskContains(removed.notes[firstEmpty], 4))
    }

    @Test
    fun `repeating the same digit clears the cell`() {
        val correct = puzzle.solution[firstEmpty]
        val filled = sessionOn(firstEmpty).withDigit(correct)
        val cleared = filled.copy(selected = firstEmpty).withDigit(correct)
        assertEquals(0, cleared.entries[firstEmpty])
    }

    @Test
    fun `a correct digit clears that note from its peers`() {
        val correct = puzzle.solution[firstEmpty]
        val peer = com.killerduel.app.core.PEERS[firstEmpty].first { puzzle.givens[it] == 0 }

        val notes = base.notes.toMutableList().also { it[peer] = bit(correct) }
        val after = base.copy(selected = firstEmpty, notes = notes).withDigit(correct)

        assertFalse(maskContains(after.notes[peer], correct))
    }

    @Test
    fun `undo restores the previous position`() {
        val correct = puzzle.solution[firstEmpty]
        val after = sessionOn(firstEmpty).withDigit(correct).withUndo()
        assertEquals(0, after.entries[firstEmpty])
        assertTrue(after.history.isEmpty())
    }

    @Test
    fun `hint fills a cell and is limited`() {
        var session = base.copy(selected = firstEmpty)
        session = session.withHint()
        assertEquals(puzzle.solution[firstEmpty], session.entries[firstEmpty])
        assertEquals(GameSession.MAX_HINTS - 1, session.hintsLeft)

        session = session.copy(hintsLeft = 0, selected = -1)
        val blocked = session.withHint()
        assertEquals(session.entries, blocked.entries)
    }

    @Test
    fun `filling the grid wins the game`() {
        var session = base
        for (cell in 0 until 81) {
            if (puzzle.givens[cell] != 0) continue
            session = session.copy(selected = cell).withDigit(puzzle.solution[cell])
        }
        assertEquals(Outcome.WON, session.outcome)
        assertEquals(81, session.filledCount)
        assertEquals(puzzle.givens.count { it == 0 }, session.moveLog.size)
    }

    @Test
    fun `the duel is lost when the opponent finishes first`() {
        val plan = SyntheticOpponentEngine().plan(puzzle, seed = 8L)
        val session = base.copy(mode = GameMode.DUEL, opponent = plan)

        assertNull(session.copy(elapsedMillis = 0).withOpponentCheck().outcome)
        assertEquals(
            Outcome.LOST_ON_TIME,
            session.copy(elapsedMillis = plan.totalMillis + 1).withOpponentCheck().outcome
        )
    }

    @Test
    fun `opponent progress counts the given digits too`() {
        val plan = SyntheticOpponentEngine().plan(puzzle, seed = 9L)
        val givens = puzzle.givens.count { it != 0 }
        val session = base.copy(mode = GameMode.DUEL, opponent = plan, elapsedMillis = 0)

        assertEquals(givens, session.opponentFilled)
        assertEquals(81, session.copy(elapsedMillis = plan.totalMillis).opponentFilled)
    }

    @Test
    fun `a finished game ignores further input`() {
        val finished = base.copy(selected = firstEmpty, outcome = Outcome.WON)
        assertEquals(finished.entries, finished.withDigit(1).entries)
        assertEquals(finished.entries, finished.withHint().entries)
    }
}
