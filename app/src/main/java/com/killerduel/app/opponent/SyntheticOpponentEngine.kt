package com.killerduel.app.opponent

import com.killerduel.app.core.Difficulty
import com.killerduel.app.core.Puzzle
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Adversaire fictif. Il ne « résout » rien : il connaît l'ordre dans lequel les
 * cases tombent pour un joueur qui raisonne (calculé à la génération de la
 * grille) et il pose ses chiffres selon un rythme crédible.
 *
 * Le rythme n'est pas régulier : le temps passé sur une case suit son coût de
 * déduction, il est bruité, et de vraies pauses de réflexion s'y intercalent.
 */
class SyntheticOpponentEngine : OpponentEngine {

    override fun plan(puzzle: Puzzle, seed: Long): OpponentPlan {
        val rng = Random(seed)
        val profile = randomProfile(rng, puzzle.difficulty)
        val pace = buildPace(puzzle, rng, profile.rating)
        return OpponentPlan(profile, buildMoves(puzzle, pace))
    }

    /** Construit le rythme puis le convertit en coups datés sur la grille. */
    private fun buildPace(puzzle: Puzzle, rng: Random, rating: Int): PaceProfile {
        val cells = puzzle.solveOrder
        if (cells.isEmpty()) return PaceProfile(emptyList(), PaceSource.SYNTHETIC, "—")

        val costs = puzzle.solveCost
        val weights = DoubleArray(cells.size) { i ->
            val cost = costs.getOrElse(i) { 1 }.toDouble()
            // Une case évidente coûte une unité, une case forcée en coûte plusieurs.
            1.0 + cost * 0.55
        }

        val target = targetMillis(puzzle.difficulty, rating, rng)
        val weightSum = weights.sum()

        val gaps = ArrayList<Long>(cells.size)
        for (i in cells.indices) {
            val share = weights[i] / weightSum * target
            val noise = exp(rng.nextDouble(-1.0, 1.0) * NOISE_SIGMA)
            var gap = share * noise
            // Quelques blocages : l'adversaire relit la grille avant de repartir.
            if (rng.nextDouble() < PAUSE_CHANCE) {
                gap += rng.nextDouble(PAUSE_MIN_MS, PAUSE_MAX_MS)
            }
            gaps.add(gap.roundToLong().coerceAtLeast(MIN_GAP_MS))
        }
        // Le premier coup arrive après une lecture de la grille.
        gaps[0] = gaps[0] + rng.nextLong(OPENING_MIN_MS, OPENING_MAX_MS)

        return PaceProfile(gaps, PaceSource.SYNTHETIC, "adversaire simulé")
    }

    /** Durée visée pour finir la grille, selon le niveau et la force de l'adversaire. */
    private fun targetMillis(difficulty: Difficulty, rating: Int, rng: Random): Double {
        val base = when (difficulty) {
            Difficulty.EASY -> 250_000.0
            Difficulty.MEDIUM -> 430_000.0
            Difficulty.HARD -> 680_000.0
            Difficulty.KILLER -> 950_000.0
        }
        // Un rating élevé raccourcit la partie, sans jamais la rendre irréaliste.
        val skill = 1.0 - (rating - RATING_MIN).toDouble() / (RATING_MAX - RATING_MIN) * 0.42
        return base * skill * rng.nextDouble(0.82, 1.24)
    }

    private fun randomProfile(rng: Random, difficulty: Difficulty): OpponentProfile {
        val (name, flag) = OPPONENTS[rng.nextInt(OPPONENTS.size)]
        val floor = when (difficulty) {
            Difficulty.EASY -> RATING_MIN
            Difficulty.MEDIUM -> RATING_MIN + 200
            Difficulty.HARD -> RATING_MIN + 400
            Difficulty.KILLER -> RATING_MIN + 600
        }
        val rating = rng.nextInt(floor, (floor + 500).coerceAtMost(RATING_MAX))
        return OpponentProfile(name, flag, rating, PaceSource.SYNTHETIC)
    }

    companion object {
        private const val NOISE_SIGMA = 0.75
        private const val PAUSE_CHANCE = 0.07
        private const val PAUSE_MIN_MS = 2_500.0
        private const val PAUSE_MAX_MS = 11_000.0
        private const val MIN_GAP_MS = 700L
        private const val OPENING_MIN_MS = 6_000L
        private const val OPENING_MAX_MS = 17_000L

        const val RATING_MIN = 900
        const val RATING_MAX = 2400

        /** Assez de pseudos pour qu'un adversaire ne se répète pas d'une partie à l'autre. */
        private val OPPONENTS = listOf(
            "Mika" to "🇯🇵", "Lucia" to "🇪🇸", "Bram" to "🇳🇱", "Théo" to "🇫🇷",
            "Ingrid" to "🇸🇪", "Rafa" to "🇧🇷", "Nadia" to "🇲🇦", "Sam" to "🇬🇧",
            "Yara" to "🇱🇧", "Kenji" to "🇯🇵", "Olek" to "🇵🇱", "Priya" to "🇮🇳",
            "Dana" to "🇺🇸", "Matteo" to "🇮🇹", "Elif" to "🇹🇷", "Anton" to "🇩🇪",
            "Chloé" to "🇧🇪", "Nuno" to "🇵🇹", "Hana" to "🇰🇷", "Iris" to "🇦🇺",
            "Selin" to "🇹🇷", "Pablo" to "🇦🇷", "Aoife" to "🇮🇪", "Tomas" to "🇨🇿"
        )
    }
}

/** Transforme un rythme en coups datés sur l'ordre de déduction de la grille. */
internal fun buildMoves(puzzle: Puzzle, pace: PaceProfile): List<OpponentMove> {
    val cells = puzzle.solveOrder
    val gaps = pace.resampledTo(cells.size)
    val moves = ArrayList<OpponentMove>(cells.size)
    var clock = 0L
    for (i in cells.indices) {
        clock += gaps.getOrElse(i) { 1_000L }
        val cell = cells[i]
        moves.add(OpponentMove(clock, cell, puzzle.solution[cell]))
    }
    return moves
}
