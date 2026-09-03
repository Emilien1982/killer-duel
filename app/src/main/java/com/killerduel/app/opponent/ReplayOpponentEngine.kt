package com.killerduel.app.opponent

import com.killerduel.app.core.Puzzle
import com.killerduel.app.data.RecordedSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Adversaire tiré d'une partie d'entraînement réellement jouée. On n'en rejoue
 * pas les coups — ils appartiennent à une autre grille — mais son rythme, appliqué
 * à l'ordre de déduction de la grille du jour. La cadence, les hésitations et le
 * temps total sont donc ceux d'un vrai joueur.
 */
class ReplayOpponentEngine(private val session: RecordedSession) : OpponentEngine {

    override fun plan(puzzle: Puzzle, seed: Long): OpponentPlan {
        val pace = paceOf(session)
        val profile = OpponentProfile(
            name = "Rejeu du ${DATE_FORMAT.format(Date(session.completedAtEpochMillis))}",
            flag = "⏱",
            rating = ratingFor(session),
            origin = PaceSource.RECORDED
        )
        // Le seed ne sert qu'à ne pas figer complètement un rejeu répété.
        val jitter = Random(seed).nextDouble(0.94, 1.06)
        val jittered = pace.copy(gapsMillis = pace.gapsMillis.map { (it * jitter).toLong() })
        return OpponentPlan(profile, buildMoves(puzzle, jittered))
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("d MMM", Locale.FRANCE)

        /** Extrait le rythme d'une partie enregistrée : les écarts entre coups. */
        fun paceOf(session: RecordedSession): PaceProfile {
            val ordered = session.moves.sortedBy { it.elapsedMillis }
            val gaps = ArrayList<Long>(ordered.size)
            var previous = 0L
            for (move in ordered) {
                gaps.add((move.elapsedMillis - previous).coerceAtLeast(150L))
                previous = move.elapsedMillis
            }
            return PaceProfile(gaps, PaceSource.RECORDED, "partie enregistrée")
        }

        /** Un temps rapide vaut un rating élevé, borné sur la même échelle que le simulé. */
        fun ratingFor(session: RecordedSession): Int {
            val minutes = session.durationMillis / 60_000.0
            val raw = 2300 - minutes * 55 - session.mistakes * 40
            return raw.toInt().coerceIn(SyntheticOpponentEngine.RATING_MIN, SyntheticOpponentEngine.RATING_MAX)
        }
    }
}
