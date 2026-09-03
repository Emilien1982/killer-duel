package com.killerduel.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class EngineTest {

    @Test
    fun `full solution respects sudoku constraints`() {
        repeat(20) { i ->
            val grid = PuzzleGenerator.fullSolution(Random(i.toLong()))!!
            assertTrue(grid.all { it in 1..9 })
            for (unit in UNITS) {
                assertEquals("unité incomplète", 9, unit.map { grid[it] }.toSet().size)
            }
        }
    }

    @Test
    fun `combinations table is consistent`() {
        assertEquals(listOf(bit(1) or bit(2)), Combinations.of(2, 3).toList())
        assertEquals(1, Combinations.of(9, 45).size)
        assertEquals(0, Combinations.of(2, 18).size)
        for (size in 1..9) {
            for (sum in 0..45) {
                Combinations.of(size, sum).forEach { mask ->
                    assertEquals(size, Integer.bitCount(mask))
                    assertEquals(sum, (1..9).filter { maskContains(mask, it) }.sum())
                }
            }
        }
    }

    @Test
    fun `generated puzzles are well formed and uniquely solvable`() {
        for (difficulty in Difficulty.entries) {
            val elapsed = measureTimeMillis {
                repeat(3) { i ->
                    val puzzle = PuzzleGenerator.generate(difficulty, seed = 1000L + i)
                    assertCoversGrid(puzzle)
                    assertCagesConsistent(puzzle)
                    assertGivensMatchSolution(puzzle)

                    val result = KillerSolver(puzzle.cages, puzzle.givens.toIntArray()).solve(2)
                    assertEquals(
                        "$difficulty devrait avoir une solution unique",
                        1, result.solutionCount
                    )
                    assertTrue(result.solution!!.contentEquals(puzzle.solution.toIntArray()))

                    val playable = puzzle.givens.count { it == 0 }
                    assertTrue("$difficulty: trop peu de cases à remplir ($playable)", playable >= 40)

                    assertEquals(
                        "$difficulty: l'ordre de déduction doit couvrir toutes les cases vides",
                        playable, puzzle.solveOrder.size
                    )
                    assertEquals(playable, puzzle.solveOrder.toSet().size)
                }
            }
            println("$difficulty : 3 grilles en ${elapsed} ms (${elapsed / 3} ms/grille)")
        }
    }

    @Test
    fun `easy and medium are solvable by pure logic`() {
        for (difficulty in listOf(Difficulty.EASY, Difficulty.MEDIUM)) {
            repeat(2) { i ->
                val puzzle = PuzzleGenerator.generate(difficulty, seed = 77L + i)
                val result = KillerSolver(puzzle.cages, puzzle.givens.toIntArray()).solve(1)
                assertTrue(
                    "$difficulty doit se résoudre sans recherche",
                    result.solvedByLogic
                )
            }
        }
    }

    @Test
    fun `killer level gives nothing away`() {
        val puzzle = PuzzleGenerator.generate(Difficulty.KILLER, seed = 4242L)
        assertTrue("aucun chiffre donné attendu", puzzle.givens.all { it == 0 })
        assertTrue("aucune cage d'une case", puzzle.cages.none { it.size == 1 })
    }

    private fun assertCoversGrid(puzzle: Puzzle) {
        val seen = IntArray(81)
        puzzle.cages.forEach { cage -> cage.cells.forEach { seen[it]++ } }
        assertTrue("chaque case appartient à exactement une cage", seen.all { it == 1 })
    }

    private fun assertCagesConsistent(puzzle: Puzzle) {
        puzzle.cages.forEach { cage ->
            val digits = cage.cells.map { puzzle.solution[it] }
            assertEquals("chiffres répétés dans une cage", digits.size, digits.toSet().size)
            assertEquals("somme de cage incorrecte", cage.sum, digits.sum())
            assertTrue("cage non contiguë", isConnected(cage.cells))
        }
    }

    private fun assertGivensMatchSolution(puzzle: Puzzle) {
        puzzle.givens.forEachIndexed { i, g ->
            if (g != 0) assertEquals(puzzle.solution[i], g)
        }
    }

    private fun isConnected(cells: List<Int>): Boolean {
        val set = cells.toHashSet()
        val stack = ArrayDeque(listOf(cells.first()))
        val seen = hashSetOf(cells.first())
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            for (n in ADJACENT[cur]) if (n in set && seen.add(n)) stack.addLast(n)
        }
        return seen.size == cells.size
    }
}
