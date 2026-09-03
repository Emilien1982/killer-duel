package com.killerduel.app.core

import kotlin.random.Random

/**
 * Génération d'une grille Killer complète : solution, découpage en cages,
 * puis retrait de chiffres donnés tant que la solution reste unique.
 *
 * Le contrat de difficulté est explicite : taille des cages + nombre de chiffres
 * laissés en clair, et pour les deux premiers niveaux l'exigence que la grille
 * soit résoluble sans aucune recherche à l'aveugle.
 */
object PuzzleGenerator {

    private data class Spec(
        val minCageSize: Int,
        val maxCageSize: Int,
        val targetGivens: Int,
        val requirePureLogic: Boolean
    )

    private fun specFor(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> Spec(2, 3, 32, true)
        Difficulty.MEDIUM -> Spec(2, 4, 22, true)
        Difficulty.HARD -> Spec(2, 4, 10, false)
        Difficulty.KILLER -> Spec(2, 5, 0, false)
    }

    fun generate(difficulty: Difficulty, seed: Long = Random.nextLong()): Puzzle {
        val rng = Random(seed)
        val spec = specFor(difficulty)

        var best: Pair<List<Cage>, IntArray>? = null
        var bestGivens = Int.MAX_VALUE

        outer@ for (attempt in 0 until SOLUTION_ATTEMPTS) {
            val solution = fullSolution(rng) ?: continue
            for (cageAttempt in 0 until CAGE_ATTEMPTS) {
                val cages = buildCages(solution, rng, spec)
                if (cages.isEmpty()) continue

                if (spec.targetGivens == 0) {
                    // Une vraie grille Killer ne montre aucun chiffre : le découpage
                    // doit déterminer la grille à lui seul, sinon on en essaie un autre.
                    val empty = IntArray(81)
                    val bare = KillerSolver(cages, empty).solve(2, nodeBudget = NODE_BUDGET)
                    if (!bare.aborted && bare.solutionCount == 1) {
                        best = cages to empty
                        bestGivens = 0
                        break@outer
                    }
                    continue
                }

                val givens = carve(cages, solution, spec, rng)
                val count = givens.count { it != 0 }
                if (count < bestGivens) {
                    bestGivens = count
                    best = cages to givens
                }
                if (count <= spec.targetGivens) break@outer
            }
        }

        // Filet de sécurité : aucun découpage ne s'est suffi à lui-même, on retombe
        // sur un retrait progressif classique.
        if (best == null) {
            val solution = fullSolution(rng)!!
            var cages = buildCages(solution, rng, spec)
            while (cages.isEmpty()) cages = buildCages(solution, rng, spec)
            best = cages to carve(cages, solution, spec, rng)
        }

        val (cages, givens) = best!!
        val solution = KillerSolver(cages, givens).solve(1).solution!!
        val (order, cost) = KillerSolver(cages, givens).deductionTrace(solution)

        return Puzzle(
            cages = cages,
            givens = givens.toList(),
            solution = solution.toList(),
            difficulty = difficulty,
            seed = seed,
            solveOrder = order.toList(),
            solveCost = cost.toList()
        )
    }

    /** Retire des chiffres un à un tant que la grille garde une solution unique. */
    private fun carve(
        cages: List<Cage>,
        solution: IntArray,
        spec: Spec,
        rng: Random
    ): IntArray {
        val givens = solution.copyOf()
        var remaining = 81
        for (cell in (0 until 81).shuffled(rng)) {
            if (remaining <= spec.targetGivens) break
            val backup = givens[cell]
            givens[cell] = 0
            val result = KillerSolver(cages, givens).solve(countLimit = 2, nodeBudget = NODE_BUDGET)
            val ok = !result.aborted && result.solutionCount == 1 &&
                (!spec.requirePureLogic || result.solvedByLogic)
            if (ok) remaining-- else givens[cell] = backup
        }
        return givens
    }

    private const val SOLUTION_ATTEMPTS = 4
    private const val CAGE_ATTEMPTS = 24

    /**
     * Un découpage qui demande plus que ce budget de recherche produirait une grille
     * insoluble à la main : autant en essayer un autre, c'est bien moins cher.
     */
    private const val NODE_BUDGET = 2500

    // ---- Solution complète ----

    /** Backtracking randomisé classique sur les contraintes de sudoku. */
    fun fullSolution(rng: Random): IntArray? {
        val grid = IntArray(81)
        val rowMask = IntArray(9)
        val colMask = IntArray(9)
        val boxMask = IntArray(9)

        fun place(cell: Int): Boolean {
            if (cell == 81) return true
            val r = rowOf(cell); val c = colOf(cell); val b = boxOf(cell)
            val used = rowMask[r] or colMask[c] or boxMask[b]
            val choices = (1..9).filter { !maskContains(used, it) }.shuffled(rng)
            for (d in choices) {
                val bt = bit(d)
                grid[cell] = d
                rowMask[r] = rowMask[r] or bt; colMask[c] = colMask[c] or bt; boxMask[b] = boxMask[b] or bt
                if (place(cell + 1)) return true
                grid[cell] = 0
                rowMask[r] = rowMask[r] and bt.inv(); colMask[c] = colMask[c] and bt.inv(); boxMask[b] = boxMask[b] and bt.inv()
            }
            return false
        }

        return if (place(0)) grid else null
    }

    // ---- Découpage en cages ----

    private fun buildCages(solution: IntArray, rng: Random, spec: Spec): List<Cage> {
        val cageId = IntArray(81) { -1 }
        val groups = ArrayList<MutableList<Int>>()

        val seedOrder = (0 until 81).shuffled(rng)
        for (start in seedOrder) {
            if (cageId[start] != -1) continue
            val target = rng.nextInt(spec.minCageSize, spec.maxCageSize + 1)
            val group = mutableListOf(start)
            var digits = bit(solution[start])
            cageId[start] = groups.size

            while (group.size < target) {
                val options = ArrayList<Int>(8)
                for (member in group) {
                    for (n in ADJACENT[member]) {
                        if (cageId[n] == -1 && !maskContains(digits, solution[n]) && n !in options) {
                            options.add(n)
                        }
                    }
                }
                if (options.isEmpty()) break
                val pick = options[rng.nextInt(options.size)]
                cageId[pick] = groups.size
                digits = digits or bit(solution[pick])
                group.add(pick)
            }
            groups.add(group)
        }

        absorbSingletons(groups, cageId, solution, spec)

        // Une cage d'une seule case offrirait son chiffre : découpage à rejouer.
        if (groups.any { it.size == 1 }) return emptyList()

        return groups.filter { it.isNotEmpty() }.map { cells ->
            val sorted = cells.sorted()
            Cage(sum = sorted.sumOf { solution[it] }, cells = sorted)
        }
    }

    /**
     * Une cage d'une seule case révèle son chiffre gratuitement. On les recolle
     * à une cage voisine quand c'est possible, obligatoire au niveau Killer.
     */
    private fun absorbSingletons(
        groups: ArrayList<MutableList<Int>>,
        cageId: IntArray,
        solution: IntArray,
        spec: Spec
    ) {
        for (i in groups.indices) {
            val group = groups[i]
            if (group.size != 1) continue
            val cell = group[0]
            val digit = solution[cell]

            var bestIndex = -1
            var bestSize = Int.MAX_VALUE
            val maxSize = spec.maxCageSize + 1
            for (n in ADJACENT[cell]) {
                val other = cageId[n]
                if (other == -1 || other == i) continue
                val target = groups[other]
                if (target.size >= maxSize) continue
                if (target.any { solution[it] == digit }) continue
                if (target.size < bestSize) { bestSize = target.size; bestIndex = other }
            }
            if (bestIndex != -1) {
                groups[bestIndex].add(cell)
                cageId[cell] = bestIndex
                group.clear()
            }
        }
    }
}
