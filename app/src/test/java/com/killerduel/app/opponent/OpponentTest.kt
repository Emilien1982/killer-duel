package com.killerduel.app.opponent

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.PuzzleGenerator
import com.killerduel.app.data.RecordedMove
import com.killerduel.app.data.RecordedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpponentTest {

    private val puzzle = PuzzleGenerator.generate(Difficulty.MEDIUM, seed = 55L)

    @Test
    fun `synthetic plan fills every empty cell in order`() {
        val plan = SyntheticOpponentEngine().plan(puzzle, seed = 1L)
        val empties = puzzle.givens.count { it == 0 }

        assertEquals(empties, plan.moves.size)
        assertEquals(empties, plan.moves.map { it.cell }.toSet().size)

        var previous = -1L
        plan.moves.forEach { move ->
            assertTrue("les coups doivent être strictement datés", move.atMillis > previous)
            previous = move.atMillis
            assertEquals(puzzle.solution[move.cell], move.value)
            assertEquals(0, puzzle.givens[move.cell])
        }
    }

    @Test
    fun `synthetic durations stay in a believable range per difficulty`() {
        val bounds = mapOf(
            Difficulty.EASY to (90_000L..600_000L),
            Difficulty.MEDIUM to (150_000L..900_000L),
            Difficulty.HARD to (240_000L..1_500_000L),
            Difficulty.KILLER to (300_000L..2_100_000L)
        )
        for ((difficulty, range) in bounds) {
            val grid = PuzzleGenerator.generate(difficulty, seed = 909L)
            repeat(12) { i ->
                val total = SyntheticOpponentEngine().plan(grid, seed = i.toLong()).totalMillis
                assertTrue(
                    "$difficulty: ${total / 1000}s hors de la fourchette attendue",
                    total in range
                )
            }
        }
    }

    @Test
    fun `progress lookup matches the timeline`() {
        val plan = SyntheticOpponentEngine().plan(puzzle, seed = 3L)
        assertEquals(0, plan.filledAt(0))
        assertEquals(1, plan.filledAt(plan.moves[0].atMillis))
        assertEquals(plan.moves.size, plan.filledAt(plan.totalMillis + 1))
        for (i in plan.moves.indices) {
            assertEquals(i + 1, plan.filledAt(plan.moves[i].atMillis))
        }
    }

    @Test
    fun `resampling preserves total duration`() {
        val pace = PaceProfile(listOf(1000L, 3000L, 2000L, 8000L), PaceSource.RECORDED, "t")
        listOf(2, 4, 9, 40).forEach { size ->
            val out = pace.resampledTo(size)
            assertEquals(size, out.size)
            val drift = kotlin.math.abs(out.sum() - pace.totalMillis)
            assertTrue("dérive de $drift ms sur $size coups", drift <= size)
        }
    }

    @Test
    fun `replay opponent keeps the pace of the recorded game`() {
        val recorded = fakeSession(durationMillis = 400_000L, moveCount = 30)
        val plan = ReplayOpponentEngine(recorded).plan(puzzle, seed = 12L)

        assertEquals(puzzle.givens.count { it == 0 }, plan.moves.size)
        assertEquals(PaceSource.RECORDED, plan.profile.origin)

        val ratio = plan.totalMillis.toDouble() / recorded.durationMillis
        assertTrue("durée rejouée trop éloignée de l'original ($ratio)", ratio in 0.9..1.1)
    }

    @Test
    fun `replay rating rewards fast and clean games`() {
        val fast = ReplayOpponentEngine.ratingFor(fakeSession(300_000L, 10, mistakes = 0))
        val slow = ReplayOpponentEngine.ratingFor(fakeSession(1_500_000L, 10, mistakes = 0))
        val sloppy = ReplayOpponentEngine.ratingFor(fakeSession(300_000L, 10, mistakes = 3))
        assertTrue(fast > slow)
        assertTrue(fast > sloppy)
    }

    private fun fakeSession(durationMillis: Long, moveCount: Int, mistakes: Int = 0) =
        RecordedSession(
            id = "test",
            difficulty = Difficulty.MEDIUM,
            puzzleSeed = 1L,
            completedAtEpochMillis = 1_700_000_000_000L,
            durationMillis = durationMillis,
            mistakes = mistakes,
            moves = (1..moveCount).map {
                RecordedMove(durationMillis * it / moveCount, it, 1)
            }
        )
}
