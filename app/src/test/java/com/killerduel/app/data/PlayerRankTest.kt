package com.killerduel.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRankTest {

    @Test
    fun `stars raise the level within the same month`() {
        val start = PlayerRank(level = 10, monthKey = "2026-09")
        assertEquals(13, start.advance("2026-09", 3).level)
        assertEquals(10, start.advance("2026-09", 0).level)
    }

    @Test
    fun `a new month restarts at half the level reached`() {
        val reached = PlayerRank(level = 100, monthKey = "2026-09")
        val next = reached.advance("2026-10", 0)
        assertEquals(50, next.level)
        assertEquals("2026-10", next.monthKey)

        assertEquals(20, PlayerRank(level = 40, monthKey = "2026-09").advance("2026-10", 0).level)
        assertEquals(23, PlayerRank(level = 40, monthKey = "2026-09").advance("2026-10", 3).level)
    }

    @Test
    fun `the level never leaves its bounds`() {
        assertEquals(
            PlayerRank.MAX_LEVEL,
            PlayerRank(level = 99, monthKey = "2026-09").advance("2026-09", 3).level
        )
        assertEquals(
            PlayerRank.MIN_LEVEL,
            PlayerRank(level = 1, monthKey = "2026-09").advance("2026-10", 0).level
        )
    }

    @Test
    fun `the very first month keeps the starting level`() {
        val fresh = PlayerRank()
        val started = fresh.advance("2026-09", 2)
        assertEquals(3, started.level)
        assertEquals("2026-09", started.monthKey)
    }

    @Test
    fun `several months in a row halve repeatedly`() {
        var rank = PlayerRank(level = 80, monthKey = "2026-09")
        rank = rank.advance("2026-10", 0)
        assertEquals(40, rank.level)
        rank = rank.advance("2026-11", 0)
        assertEquals(20, rank.level)
    }
}
