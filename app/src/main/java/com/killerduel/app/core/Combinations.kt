package com.killerduel.app.core

/**
 * Table des combinaisons de chiffres distincts : pour une taille et une somme
 * données, tous les masques de bits possibles. 512 sous-ensembles au total,
 * calculés une fois pour toutes — c'est le levier principal du solveur Killer.
 */
object Combinations {

    /** [size][sum] -> masques valides. */
    private val table: Array<Array<IntArray>> = run {
        val buckets = Array(10) { Array(46) { mutableListOf<Int>() } }
        for (mask in 1 until 512) {
            var size = 0
            var sum = 0
            for (d in 1..9) if (maskContains(mask, d)) { size++; sum += d }
            buckets[size][sum].add(mask)
        }
        Array(10) { s -> Array(46) { v -> buckets[s][v].toIntArray() } }
    }

    /** Masques de [size] chiffres distincts dont la somme vaut [sum]. */
    fun of(size: Int, sum: Int): IntArray {
        if (size !in 1..9 || sum !in 0..45) return EMPTY
        return table[size][sum]
    }

    /** Somme minimale atteignable avec [size] chiffres distincts. */
    fun minSum(size: Int) = size * (size + 1) / 2

    /** Somme maximale atteignable avec [size] chiffres distincts. */
    fun maxSum(size: Int) = size * (19 - size) / 2

    private val EMPTY = IntArray(0)
}
