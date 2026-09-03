package com.killerduel.app.opponent

import com.killerduel.app.core.Puzzle

/** Identité affichée de l'adversaire pendant un duel. */
data class OpponentProfile(
    val name: String,
    val flag: String,
    val rating: Int,
    val origin: PaceSource
)

/** Un chiffre posé par l'adversaire, daté depuis le début du duel. */
data class OpponentMove(val atMillis: Long, val cell: Int, val value: Int)

/** Le déroulé complet d'un adversaire, connu d'avance et rejoué en temps réel. */
data class OpponentPlan(
    val profile: OpponentProfile,
    val moves: List<OpponentMove>
) {
    val totalMillis: Long get() = moves.lastOrNull()?.atMillis ?: 0L

    /** Nombre de cases posées à l'instant [elapsedMillis]. */
    fun filledAt(elapsedMillis: Long): Int {
        var lo = 0
        var hi = moves.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (moves[mid].atMillis <= elapsedMillis) lo = mid + 1 else hi = mid
        }
        return lo
    }
}

/** D'où vient le rythme de l'adversaire. */
enum class PaceSource { SYNTHETIC, RECORDED }

/**
 * Le rythme d'un joueur, réduit à ce qui compte : les intervalles entre coups
 * successifs. Un rythme est indépendant de la grille, ce qui permet de rejouer
 * une vraie partie enregistrée sur une grille que le joueur n'a jamais vue.
 */
data class PaceProfile(
    val gapsMillis: List<Long>,
    val source: PaceSource,
    val label: String
) {
    val totalMillis: Long get() = gapsMillis.sum()

    /**
     * Étire ou comprime le rythme pour qu'il compte exactement [count] coups,
     * en conservant la durée totale et la forme de la courbe.
     */
    fun resampledTo(count: Int): List<Long> {
        if (count <= 0) return emptyList()
        if (gapsMillis.size == count) return gapsMillis
        if (gapsMillis.isEmpty()) return List(count) { 0L }

        val source = gapsMillis
        val out = ArrayList<Long>(count)
        for (i in 0 until count) {
            val pos = i.toDouble() * source.size / count
            val idx = pos.toInt().coerceAtMost(source.size - 1)
            out.add(source[idx])
        }
        // Le rééchantillonnage déforme la somme : on la ramène à la durée d'origine.
        val sum = out.sum()
        if (sum == 0L) return out
        val factor = totalMillis.toDouble() / sum
        return out.map { (it * factor).toLong() }
    }
}

/** Produit le déroulé d'un adversaire pour une grille donnée. */
interface OpponentEngine {
    fun plan(puzzle: Puzzle, seed: Long): OpponentPlan
}
