package com.killerduel.app.core

import org.junit.Test
import kotlin.system.measureTimeMillis

class PerfTest {
    @Test
    fun `killer generation stays responsive`() {
        val times = (2000L until 2040L).map { seed ->
            var puzzle: Puzzle? = null
            val ms = measureTimeMillis { puzzle = PuzzleGenerator.generate(Difficulty.KILLER, seed) }
            require(puzzle!!.givens.all { it == 0 })
            ms
        }
        println("KILLER sur 40 grilles : médiane ${times.sorted()[20]} ms, pire ${times.max()} ms, total ${times.sum()} ms")
    }
}
