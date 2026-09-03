package com.killerduel.app.core

import kotlinx.serialization.Serializable

/** Niveaux de difficulté, alignés sur la référence du genre. */
enum class Difficulty(val label: String) {
    EASY("Facile"),
    MEDIUM("Moyen"),
    HARD("Difficile"),
    KILLER("Killer");
}

/**
 * Une cage : un groupe de cellules contiguës dont la somme est imposée
 * et à l'intérieur duquel un chiffre ne peut pas se répéter.
 * [cells] contient des index 0..80 (ligne * 9 + colonne), triés croissants.
 */
@Serializable
data class Cage(val sum: Int, val cells: List<Int>) {
    val size: Int get() = cells.size

    /** Cellule où s'affiche la somme : la plus haute puis la plus à gauche. */
    val anchor: Int get() = cells.first()
}

/**
 * Une grille jouable. [givens] vaut 0 pour une case vide.
 * La solution est conservée : elle sert à la validation immédiate des erreurs
 * (comme dans le jeu de référence) et au pilotage de l'adversaire simulé.
 */
@Serializable
data class Puzzle(
    val cages: List<Cage>,
    val givens: List<Int>,
    val solution: List<Int>,
    val difficulty: Difficulty,
    val seed: Long,
    /** Ordre dans lequel un solveur logique déduit les cases, du plus évident au plus dur. */
    val solveOrder: List<Int>,
    /** Coût relatif de chaque déduction, indexé comme [solveOrder]. */
    val solveCost: List<Int>
) {
    val cageOfCell: IntArray by lazy {
        val map = IntArray(81) { -1 }
        cages.forEachIndexed { i, cage -> cage.cells.forEach { map[it] = i } }
        map
    }

    companion object {
        const val N = 9
        const val CELLS = 81
    }
}

// ---- Helpers de géométrie de la grille ----

inline fun rowOf(cell: Int) = cell / 9
inline fun colOf(cell: Int) = cell % 9
inline fun boxOf(cell: Int) = (cell / 27) * 3 + (cell % 9) / 3

/** Les 20 cellules qui partagent une ligne, une colonne ou une région avec [cell]. */
val PEERS: Array<IntArray> = Array(81) { cell ->
    val r = rowOf(cell); val c = colOf(cell); val b = boxOf(cell)
    (0 until 81).filter { it != cell && (rowOf(it) == r || colOf(it) == c || boxOf(it) == b) }
        .toIntArray()
}

/** Les 27 unités de la grille : 9 lignes, 9 colonnes, 9 régions. */
val UNITS: Array<IntArray> = buildList {
    for (r in 0 until 9) add(IntArray(9) { r * 9 + it })
    for (c in 0 until 9) add(IntArray(9) { it * 9 + c })
    for (b in 0 until 9) {
        val r0 = (b / 3) * 3; val c0 = (b % 3) * 3
        add(IntArray(9) { (r0 + it / 3) * 9 + (c0 + it % 3) })
    }
}.toTypedArray()

/** Voisins orthogonaux, pour la construction des cages. */
val ADJACENT: Array<IntArray> = Array(81) { cell ->
    val r = rowOf(cell); val c = colOf(cell)
    buildList {
        if (r > 0) add(cell - 9)
        if (r < 8) add(cell + 9)
        if (c > 0) add(cell - 1)
        if (c < 8) add(cell + 1)
    }.toIntArray()
}

const val ALL_DIGITS = 0x1FF // bits 0..8 pour les chiffres 1..9

inline fun bit(digit: Int) = 1 shl (digit - 1)
inline fun maskContains(mask: Int, digit: Int) = mask and bit(digit) != 0
