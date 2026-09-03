package com.killerduel.app.core

/**
 * Solveur Killer : propagation de contraintes (singles + combinaisons de cages)
 * puis recherche en profondeur. Il sert à trois choses :
 *  - vérifier qu'une grille générée a une solution unique ;
 *  - noter sa difficulté ;
 *  - produire l'ordre de déduction utilisé par l'adversaire simulé.
 */
class KillerSolver(
    private val cages: List<Cage>,
    private val givens: IntArray
) {
    private val cageOf = IntArray(81) { -1 }

    /**
     * Cages virtuelles déduites de la règle des 45 : dans une ligne, une colonne
     * ou une région, la somme vaut 45. Les cases non couvertes par les cages
     * entièrement incluses ("innies") forment donc un groupe de somme connue,
     * dont les chiffres sont distincts puisqu'ils partagent une unité.
     */
    private val virtualCages: List<Cage>

    /** Toutes les contraintes de somme utilisées par la propagation. */
    private val constraintCages: List<Cage>

    init {
        cages.forEachIndexed { i, cage -> cage.cells.forEach { cageOf[it] = i } }

        val virtual = ArrayList<Cage>()
        for (unit in UNITS) {
            val unitSet = unit.toHashSet()
            var insideSum = 0
            val covered = HashSet<Int>()
            for (cage in cages) {
                if (unitSet.containsAll(cage.cells)) {
                    insideSum += cage.sum
                    covered.addAll(cage.cells)
                }
            }
            val innies = unit.filter { it !in covered }
            if (innies.size in 1..INNIE_LIMIT) {
                virtual.add(Cage(sum = 45 - insideSum, cells = innies.sorted()))
            }
        }
        virtualCages = virtual
        constraintCages = cages + virtual
    }

    class Result(
        val solutionCount: Int,
        val solution: IntArray?,
        /** Cases résolues par pure logique, dans l'ordre de déduction. */
        val logicalOrder: IntArray,
        /** Coût de chaque déduction (1 = évident, plus = plus subtil). */
        val logicalCost: IntArray,
        /** Nombre de nœuds explorés en recherche : 0 = grille purement logique. */
        val searchNodes: Int,
        /** Vrai si la recherche a été interrompue faute de budget. */
        val aborted: Boolean = false
    ) {
        val solvedByLogic: Boolean get() = logicalOrder.size == 81
    }

    private class State(
        val candidates: IntArray,
        val values: IntArray,
        var assigned: Int
    ) {
        fun copy() = State(candidates.copyOf(), values.copyOf(), assigned)
    }

    private var searchNodes = 0
    private var nodeBudget = Int.MAX_VALUE
    private var aborted = false
    private val order = ArrayList<Int>(81)
    private val cost = ArrayList<Int>(81)

    /**
     * @param countLimit arrête la recherche dès que ce nombre de solutions est atteint.
     */
    fun solve(countLimit: Int = 2, nodeBudget: Int = Int.MAX_VALUE): Result {
        searchNodes = 0
        this.nodeBudget = nodeBudget
        aborted = false
        order.clear()
        cost.clear()

        val state = State(IntArray(81) { ALL_DIGITS }, IntArray(81), 0)
        for (c in 0 until 81) {
            val g = givens[c]
            if (g != 0 && state.values[c] == 0) {
                if (!assign(state, c, g, recordOrder = true, cost = 0)) {
                    return Result(0, null, IntArray(0), IntArray(0), 0)
                }
            }
        }

        if (!propagate(state, recordOrder = true)) {
            return Result(0, null, IntArray(0), IntArray(0), 0)
        }

        val logicalOrder = order.toIntArray()
        val logicalCost = cost.toIntArray()

        val solutions = ArrayList<IntArray>(2)
        search(state, countLimit, solutions)

        return Result(
            solutionCount = solutions.size,
            solution = solutions.firstOrNull(),
            logicalOrder = logicalOrder,
            logicalCost = logicalCost,
            searchNodes = searchNodes,
            aborted = aborted
        )
    }

    // ---- Propagation ----

    private fun assign(
        state: State,
        cell: Int,
        digit: Int,
        recordOrder: Boolean,
        cost: Int
    ): Boolean {
        if (state.values[cell] == digit) return true
        if (state.values[cell] != 0) return false
        if (!maskContains(state.candidates[cell], digit)) return false

        state.values[cell] = digit
        state.candidates[cell] = bit(digit)
        state.assigned++
        if (recordOrder) {
            order.add(cell)
            this.cost.add(cost)
        }

        val b = bit(digit)
        for (p in PEERS[cell]) {
            if (state.candidates[p] and b != 0 && state.values[p] == 0) {
                state.candidates[p] = state.candidates[p] and b.inv()
                if (state.candidates[p] == 0) return false
            }
        }
        val cage = cages[cageOf[cell]]
        for (p in cage.cells) {
            if (p != cell && state.values[p] == 0 && state.candidates[p] and b != 0) {
                state.candidates[p] = state.candidates[p] and b.inv()
                if (state.candidates[p] == 0) return false
            }
        }
        return true
    }

    private fun propagate(state: State, recordOrder: Boolean): Boolean {
        var changed = true
        var round = 1
        while (changed) {
            changed = false

            // 1. Singles nus : une case n'a plus qu'un candidat.
            for (c in 0 until 81) {
                if (state.values[c] == 0) {
                    val m = state.candidates[c]
                    if (m == 0) return false
                    if (m and (m - 1) == 0) {
                        val d = Integer.numberOfTrailingZeros(m) + 1
                        if (!assign(state, c, d, recordOrder, round)) return false
                        changed = true
                    }
                }
            }

            // 2. Singles cachés : un chiffre n'a plus qu'une place dans une unité.
            for (unit in UNITS) {
                for (d in 1..9) {
                    val b = bit(d)
                    var count = 0
                    var target = -1
                    var placed = false
                    for (c in unit) {
                        if (state.values[c] == d) { placed = true; break }
                        if (state.values[c] == 0 && state.candidates[c] and b != 0) {
                            count++; target = c
                        }
                    }
                    if (placed) continue
                    if (count == 0) return false
                    if (count == 1) {
                        if (!assign(state, target, d, recordOrder, round)) return false
                        changed = true
                    }
                }
            }

            // 3. Contraintes de cages via les combinaisons possibles.
            if (!propagateCages(state)) return false
            if (!changed) {
                // Une passe de cages peut avoir réduit des candidats sans rien fixer :
                // on relance tant qu'un single apparaît.
                var newSingle = false
                for (c in 0 until 81) {
                    if (state.values[c] == 0) {
                        val m = state.candidates[c]
                        if (m and (m - 1) == 0) { newSingle = true; break }
                    }
                }
                if (newSingle) changed = true
            }
            round++
            if (round > 40) break
        }
        return true
    }

    private fun propagateCages(state: State): Boolean {
        for (cage in constraintCages) {
            var usedMask = 0
            var remainingSum = cage.sum
            var free = 0
            for (c in cage.cells) {
                val v = state.values[c]
                if (v != 0) {
                    val b = bit(v)
                    if (usedMask and b != 0) return false
                    usedMask = usedMask or b
                    remainingSum -= v
                } else free++
            }
            if (free == 0) {
                if (remainingSum != 0) return false
                continue
            }
            if (remainingSum < Combinations.minSum(free)) return false
            if (remainingSum > Combinations.maxSum(free)) return false

            val freeCells = IntArray(free)
            var k = 0
            for (c in cage.cells) if (state.values[c] == 0) freeCells[k++] = c

            var union = 0
            for (combo in Combinations.of(free, remainingSum)) {
                if (combo and usedMask != 0) continue
                if (free <= MATCHING_LIMIT && !matchable(combo, freeCells, state)) continue
                union = union or combo
            }
            if (union == 0) return false

            for (c in freeCells) {
                val reduced = state.candidates[c] and union
                if (reduced == 0) return false
                state.candidates[c] = reduced
            }
        }
        return true
    }

    /** Existe-t-il une affectation bijective des chiffres de [combo] aux [cells] ? (Kuhn) */
    private fun matchable(combo: Int, cells: IntArray, state: State): Boolean {
        val digits = IntArray(Integer.bitCount(combo))
        var i = 0
        for (d in 1..9) if (maskContains(combo, d)) digits[i++] = d

        val matchOfCell = IntArray(cells.size) { -1 }
        val seen = BooleanArray(cells.size)

        fun tryAssign(d: Int): Boolean {
            fun dfs(digit: Int): Boolean {
                for (j in cells.indices) {
                    if (seen[j]) continue
                    if (state.candidates[cells[j]] and bit(digit) == 0) continue
                    seen[j] = true
                    if (matchOfCell[j] == -1 || dfs(matchOfCell[j])) {
                        matchOfCell[j] = digit
                        return true
                    }
                }
                return false
            }
            java.util.Arrays.fill(seen, false)
            return dfs(d)
        }

        for (d in digits) if (!tryAssign(d)) return false
        return true
    }

    // ---- Recherche ----

    private fun search(state: State, limit: Int, out: MutableList<IntArray>) {
        if (out.size >= limit || aborted) return
        if (state.assigned == 81) {
            out.add(state.values.copyOf())
            return
        }

        var best = -1
        var bestCount = 10
        for (c in 0 until 81) {
            if (state.values[c] == 0) {
                val n = Integer.bitCount(state.candidates[c])
                if (n < bestCount) { bestCount = n; best = c; if (n == 2) break }
            }
        }
        if (best == -1) return

        for (d in 1..9) {
            if (!maskContains(state.candidates[best], d)) continue
            searchNodes++
            if (searchNodes > nodeBudget) { aborted = true; return }
            val next = state.copy()
            if (assign(next, best, d, recordOrder = false, cost = 0) &&
                propagate(next, recordOrder = false)
            ) {
                search(next, limit, out)
                if (out.size >= limit) return
            }
        }
    }

    /**
     * Ordre dans lequel les cases tombent pour un joueur qui raisonne :
     * on applique la logique tant qu'elle avance, et quand elle bloque on
     * "force" la case la plus contrainte (ce que fait un humain qui teste une piste).
     * Renvoie l'ordre des cases non données et le coût associé à chacune.
     */
    fun deductionTrace(solution: IntArray): Pair<IntArray, IntArray> {
        val state = State(IntArray(81) { ALL_DIGITS }, IntArray(81), 0)
        val resultOrder = ArrayList<Int>(81)
        val resultCost = ArrayList<Int>(81)

        order.clear(); cost.clear()
        for (c in 0 until 81) {
            val g = givens[c]
            if (g != 0 && state.values[c] == 0) {
                if (!assign(state, c, g, recordOrder = false, cost = 0)) return Pair(IntArray(0), IntArray(0))
            }
        }

        var guard = 0
        while (state.assigned < 81 && guard++ < 100) {
            order.clear(); cost.clear()
            if (!propagate(state, recordOrder = true)) break
            for (i in order.indices) {
                resultOrder.add(order[i])
                resultCost.add(cost[i])
            }
            if (state.assigned >= 81) break

            // Blocage logique : on force la case la plus contrainte.
            var best = -1
            var bestCount = 10
            for (c in 0 until 81) {
                if (state.values[c] == 0) {
                    val n = Integer.bitCount(state.candidates[c])
                    if (n < bestCount) { bestCount = n; best = c }
                }
            }
            if (best == -1) break
            order.clear(); cost.clear()
            if (!assign(state, best, solution[best], recordOrder = true, cost = FORCED_COST)) break
            resultOrder.add(best)
            resultCost.add(FORCED_COST)
        }
        return Pair(resultOrder.toIntArray(), resultCost.toIntArray())
    }

    companion object {
        /** Coût attribué à une case qui n'est pas déductible par les techniques de base. */
        const val FORCED_COST = 12

        /** Au-delà, un groupe d'innies n'apporte plus d'information exploitable. */
        private const val INNIE_LIMIT = 5

        /** Le matching bijectif ne vaut son coût que sur les petits groupes. */
        private const val MATCHING_LIMIT = 5
    }
}
