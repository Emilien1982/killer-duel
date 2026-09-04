package com.killerduel.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.killerduel.app.core.Puzzle
import com.killerduel.app.core.boxOf
import com.killerduel.app.core.colOf
import com.killerduel.app.core.maskContains
import com.killerduel.app.core.rowOf
import com.killerduel.app.data.GameSettings
import com.killerduel.app.ui.theme.AppPalette
import com.killerduel.app.ui.theme.Palette

/**
 * Tout ce que la grille a besoin de savoir pour se dessiner.
 *
 * Déclaré immuable : sans cela, les listes en font un type instable pour Compose,
 * qui redessinerait les 81 cases à chaque battement du chronomètre.
 */
@Immutable
data class BoardState(
    val puzzle: Puzzle,
    val entries: List<Int>,
    val notes: List<Int>,
    val selected: Int,
    val wrongCells: Set<Int>,
    val settings: GameSettings,
    val activeDigit: Int = 0,
    val revealSolution: Boolean = false
) {
    fun valueAt(cell: Int): Int {
        val given = puzzle.givens[cell]
        return if (given != 0) given else entries[cell]
    }

    /**
     * Le chiffre mis en avant sur toute la grille : celui qu'on vient d'armer
     * en mode « chiffre d'abord », sinon celui de la case choisie.
     */
    val focusDigit: Int
        get() = when {
            activeDigit != 0 -> activeDigit
            selected in 0..80 -> valueAt(selected)
            else -> 0
        }
}

@Composable
fun SudokuBoard(
    state: BoardState,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val palette = Palette
    val paints = remember(density, palette) { BoardPaints(density.density, palette) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width / 9f
                    val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                    onCellTap(row * 9 + col)
                }
            }
    ) {
        drawBoard(state, paints, palette)
    }
}

private fun DrawScope.drawBoard(state: BoardState, paints: BoardPaints, palette: AppPalette) {
    val cell = size.width / 9f
    drawRect(palette.Surface, size = Size(size.width, size.width))

    drawCells(state, cell, palette)
    if (state.settings.colorfulCages) drawCageSeams(state, cell, palette)
    else drawCageOutlines(state, cell, paints, palette)
    drawGridLines(cell, palette)
    drawCageSums(state, cell, paints)
    drawContents(state, cell, paints, palette)
}

// ---- Fond des cases : teinte de cage, puis surlignages ----

private fun DrawScope.drawCells(state: BoardState, cell: Float, palette: AppPalette) {
    val selected = state.selected
    val settings = state.settings
    val focus = state.focusDigit

    val row = if (selected in 0..80) rowOf(selected) else -1
    val col = if (selected in 0..80) colOf(selected) else -1
    val box = if (selected in 0..80) boxOf(selected) else -1

    for (c in 0 until 81) {
        var color = if (settings.colorfulCages) {
            palette.CageTints[state.puzzle.cageTints[state.puzzle.cageOfCell[c]] % palette.CageTints.size]
        } else {
            palette.Surface
        }

        // Les surlignages se posent sur la teinte de la cage au lieu de la
        // remplacer : la cage reste identifiable même une fois la case éclairée.
        if (settings.highlightRegions && selected in 0..80 &&
            (rowOf(c) == row || colOf(c) == col || boxOf(c) == box)
        ) {
            color = color.darken(0.10f)
        }
        if (settings.highlightSameNumbers && focus != 0 && state.valueAt(c) == focus) {
            color = color.mix(palette.Accent, 0.26f)
        }
        if (c in state.wrongCells) color = palette.ErrorBackground
        if (c == selected) color = palette.Selected

        drawRect(
            color = color,
            topLeft = Offset(colOf(c) * cell, rowOf(c) * cell),
            size = Size(cell, cell)
        )
    }
}

/** Fin liseré blanc entre deux cages : la couleur seule laisserait les bords flous. */
private fun DrawScope.drawCageSeams(state: BoardState, cell: Float, palette: AppPalette) {
    val cages = state.puzzle.cageOfCell
    val width = 2.4f * density

    for (c in 0 until 81) {
        val r = rowOf(c)
        val col = colOf(c)
        val x0 = col * cell
        val y0 = r * cell

        if (r > 0 && cages[c] != cages[c - 9]) {
            drawLine(palette.CageSeparator, Offset(x0, y0), Offset(x0 + cell, y0), width)
        }
        if (col > 0 && cages[c] != cages[c - 1]) {
            drawLine(palette.CageSeparator, Offset(x0, y0), Offset(x0, y0 + cell), width)
        }
    }
}

/**
 * Contour pointillé, conservé pour qui préfère la présentation d'origine.
 * Un côté n'est tracé que s'il donne sur une autre cage, et il est prolongé
 * jusqu'au bord quand le côté perpendiculaire appartient à la même cage :
 * sans cela les angles resteraient ouverts.
 */
private fun DrawScope.drawCageOutlines(state: BoardState, cell: Float, paints: BoardPaints, palette: AppPalette) {
    val inset = 3.5f * density
    val cages = state.puzzle.cageOfCell

    fun same(a: Int, b: Int) = b in 0..80 && cages[a] == cages[b]

    for (c in 0 until 81) {
        val r = rowOf(c)
        val col = colOf(c)
        val x0 = col * cell
        val y0 = r * cell
        val x1 = x0 + cell
        val y1 = y0 + cell

        val openLeft = same(c, if (col > 0) c - 1 else -1)
        val openRight = same(c, if (col < 8) c + 1 else -1)
        val openUp = same(c, if (r > 0) c - 9 else -1)
        val openDown = same(c, if (r < 8) c + 9 else -1)

        val lx = if (openLeft) x0 else x0 + inset
        val rx = if (openRight) x1 else x1 - inset
        val ty = if (openUp) y0 else y0 + inset
        val by = if (openDown) y1 else y1 - inset

        if (!openUp) dash(Offset(lx, y0 + inset), Offset(rx, y0 + inset), paints, palette)
        if (!openDown) dash(Offset(lx, y1 - inset), Offset(rx, y1 - inset), paints, palette)
        if (!openLeft) dash(Offset(x0 + inset, ty), Offset(x0 + inset, by), paints, palette)
        if (!openRight) dash(Offset(x1 - inset, ty), Offset(x1 - inset, by), paints, palette)
    }
}

private fun DrawScope.dash(from: Offset, to: Offset, paints: BoardPaints, palette: AppPalette) {
    drawLine(palette.CageDash, from, to, paints.cageWidth, pathEffect = paints.cageDash)
}

private fun DrawScope.drawGridLines(cell: Float, palette: AppPalette) {
    val thin = 1f * density
    val thick = 2.5f * density

    for (i in 1..8) {
        if (i % 3 == 0) continue
        val p = i * cell
        drawLine(palette.GridLine, Offset(p, 0f), Offset(p, cell * 9), thin)
        drawLine(palette.GridLine, Offset(0f, p), Offset(cell * 9, p), thin)
    }
    for (i in 0..3) {
        val p = (i * 3 * cell).coerceIn(thick / 2, cell * 9 - thick / 2)
        drawLine(palette.GridStrong, Offset(p, 0f), Offset(p, cell * 9), thick)
        drawLine(palette.GridStrong, Offset(0f, p), Offset(cell * 9, p), thick)
    }
}

/** La somme s'affiche dans la case la plus haute puis la plus à gauche de la cage. */
private fun DrawScope.drawCageSums(state: BoardState, cell: Float, paints: BoardPaints) {
    val inset = 3.5f * density
    val canvas = drawContext.canvas.nativeCanvas
    state.puzzle.cages.forEach { cage ->
        val anchor = cage.anchor
        val x = colOf(anchor) * cell + inset + 2f * density
        val y = rowOf(anchor) * cell + inset + paints.cageSum.textSize
        canvas.drawText(cage.sum.toString(), x, y, paints.cageSum)
    }
}

// ---- Chiffres et notes ----

private fun DrawScope.drawContents(state: BoardState, cell: Float, paints: BoardPaints, palette: AppPalette) {
    val canvas = drawContext.canvas.nativeCanvas

    for (c in 0 until 81) {
        val x = colOf(c) * cell + cell / 2f
        val y = rowOf(c) * cell + cell / 2f

        val given = state.puzzle.givens[c]
        val entry = state.entries[c]
        val shown = when {
            given != 0 -> given
            entry != 0 -> entry
            state.revealSolution -> state.puzzle.solution[c]
            else -> 0
        }

        if (shown != 0) {
            val paint = when {
                given != 0 -> paints.given
                c in state.wrongCells -> paints.wrong
                state.revealSolution && entry == 0 -> paints.revealed
                else -> paints.entry
            }
            paint.textSize = cell * 0.62f
            canvas.drawText(shown.toString(), x, y - (paint.ascent() + paint.descent()) / 2f, paint)
        } else {
            val notes = state.notes[c]
            if (notes != 0) {
                val isAnchor = state.puzzle.cages[state.puzzle.cageOfCell[c]].anchor == c
                drawNotes(state, notes, colOf(c) * cell, rowOf(c) * cell, cell, paints, palette, isAnchor)
            }
        }
    }
}

/**
 * Dans la case qui porte la somme de sa cage, la première note viendrait se
 * glisser sous ce nombre : on descend alors toute la grille des notes.
 *
 * La note qui porte le chiffre mis en avant reçoit une pastille de fond, comme
 * dans les jeux du genre : c'est ce qui permet de repérer d'un regard où un
 * chiffre reste possible.
 */
private fun DrawScope.drawNotes(
    state: BoardState,
    notes: Int,
    left: Float,
    top: Float,
    cell: Float,
    paints: BoardPaints,
    palette: AppPalette,
    avoidCageSum: Boolean
) {
    val canvas = drawContext.canvas.nativeCanvas
    val topInset = if (avoidCageSum) cell * 0.22f else 0f
    val stepX = cell / 3f
    val stepY = (cell - topInset) / 3f
    paints.note.textSize = if (avoidCageSum) cell * 0.22f else cell * 0.26f

    val focus = if (state.settings.highlightSameNumbers) state.focusDigit else 0

    for (d in 1..9) {
        if (!maskContains(notes, d)) continue
        val i = d - 1
        val cx = left + (i % 3) * stepX + stepX / 2f
        val cy = top + topInset + (i / 3) * stepY + stepY / 2f

        if (d == focus) {
            val w = stepX * 0.78f
            val h = stepY * 0.82f
            drawRoundRect(
                color = palette.NoteHighlight,
                topLeft = Offset(cx - w / 2f, cy - h / 2f),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * density)
            )
        }

        val paint = if (d == focus) paints.noteFocused else paints.note
        paint.textSize = paints.note.textSize
        canvas.drawText(d.toString(), cx, cy - (paint.ascent() + paint.descent()) / 2f, paint)
    }
}

// ---- Utilitaires de couleur ----

private fun Color.darken(amount: Float) = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha
)

private fun Color.mix(other: Color, ratio: Float) = Color(
    red = red * (1f - ratio) + other.red * ratio,
    green = green * (1f - ratio) + other.green * ratio,
    blue = blue * (1f - ratio) + other.blue * ratio,
    alpha = alpha
)

/**
 * Peintures et effets de trait construits une fois pour toutes : mesurer 81 textes
 * et refabriquer un pointillé à chaque frame coûte pour un rendu identique.
 */
private class BoardPaints(density: Float, palette: AppPalette) {

    val cageWidth = 1.4f * density
    val cageDash: PathEffect = PathEffect.dashPathEffect(
        floatArrayOf(3.5f * density, 3f * density), 0f
    )

    private fun basePaint(color: Color, style: Int) = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        this.color = color.toArgb()
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
    }

    val given = basePaint(palette.Given, Typeface.BOLD)
    val entry = basePaint(palette.Entry, Typeface.NORMAL)
    val wrong = basePaint(palette.Error, Typeface.BOLD)
    val revealed = basePaint(palette.Success, Typeface.NORMAL)
    val note = basePaint(palette.Note, Typeface.NORMAL)
    val noteFocused = basePaint(palette.Given, Typeface.BOLD)
    val cageSum = basePaint(palette.CageSum, Typeface.BOLD).apply {
        textAlign = Paint.Align.LEFT
        textSize = 10f * density
    }
}
