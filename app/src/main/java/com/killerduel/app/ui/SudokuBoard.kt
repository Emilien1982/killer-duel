package com.killerduel.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.killerduel.app.core.Puzzle
import com.killerduel.app.core.boxOf
import com.killerduel.app.core.colOf
import com.killerduel.app.core.maskContains
import com.killerduel.app.core.rowOf
import com.killerduel.app.ui.theme.Palette

/** Tout ce que la grille a besoin de savoir pour se dessiner. */
data class BoardState(
    val puzzle: Puzzle,
    val entries: List<Int>,
    val notes: List<Int>,
    val selected: Int,
    val wrongCells: Set<Int>,
    val revealSolution: Boolean = false
) {
    fun valueAt(cell: Int): Int {
        val given = puzzle.givens[cell]
        return if (given != 0) given else entries[cell]
    }
}

@Composable
fun SudokuBoard(
    state: BoardState,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val paints = remember(density) { BoardPaints(density.density) }

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
        drawBoard(state, paints)
    }
}

private fun DrawScope.drawBoard(state: BoardState, paints: BoardPaints) {
    val cell = size.width / 9f
    drawRect(Palette.Surface, size = Size(size.width, size.width))

    drawHighlights(state, cell)
    drawGridLines(cell)
    drawCages(state, cell, paints)
    drawContents(state, cell, paints)
}

// ---- Surlignages ----

private fun DrawScope.drawHighlights(state: BoardState, cell: Float) {
    val selected = state.selected
    if (selected !in 0..80) return

    val selectedValue = state.valueAt(selected)
    val cageIndex = state.puzzle.cageOfCell[selected]
    val row = rowOf(selected)
    val col = colOf(selected)
    val box = boxOf(selected)

    for (c in 0 until 81) {
        val color = when {
            c == selected -> Palette.Selected
            state.puzzle.cageOfCell[c] == cageIndex -> Palette.SelectedCage
            selectedValue != 0 && state.valueAt(c) == selectedValue -> Palette.SameValue
            rowOf(c) == row || colOf(c) == col || boxOf(c) == box -> Palette.PeerHighlight
            else -> null
        }
        if (color != null) fillCell(c, cell, color)
    }

    state.wrongCells.forEach { fillCell(it, cell, Palette.ErrorBackground) }
}

private fun DrawScope.fillCell(cell: Int, size: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(colOf(cell) * size, rowOf(cell) * size),
        size = Size(size, size)
    )
}

// ---- Lignes ----

private fun DrawScope.drawGridLines(cell: Float) {
    val thin = 1f * density
    val thick = 2.5f * density

    for (i in 1..8) {
        if (i % 3 == 0) continue
        val p = i * cell
        drawLine(Palette.GridLine, Offset(p, 0f), Offset(p, cell * 9), thin)
        drawLine(Palette.GridLine, Offset(0f, p), Offset(cell * 9, p), thin)
    }
    for (i in 0..3) {
        val p = (i * 3 * cell).coerceIn(thick / 2, cell * 9 - thick / 2)
        drawLine(Palette.GridStrong, Offset(p, 0f), Offset(p, cell * 9), thick)
        drawLine(Palette.GridStrong, Offset(0f, p), Offset(cell * 9, p), thick)
    }
}

/**
 * Contour pointillé des cages. Chaque côté d'une case n'est tracé que s'il donne
 * sur une autre cage, et il est prolongé jusqu'au bord quand le côté perpendiculaire
 * appartient à la même cage : sans cela les angles resteraient ouverts.
 */
private fun DrawScope.drawCages(state: BoardState, cell: Float, paints: BoardPaints) {
    val inset = 3.5f * density
    val stroke = Stroke(
        width = 1.4f * density,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(3.5f * density, 3f * density), 0f
        )
    )
    val cages = state.puzzle.cageOfCell

    fun same(a: Int, b: Int) = b in 0..80 && cages[a] == cages[b]

    for (c in 0 until 81) {
        val r = rowOf(c)
        val col = colOf(c)
        val x0 = col * cell
        val y0 = r * cell
        val x1 = x0 + cell
        val y1 = y0 + cell

        val up = if (r > 0) c - 9 else -1
        val down = if (r < 8) c + 9 else -1
        val left = if (col > 0) c - 1 else -1
        val right = if (col < 8) c + 1 else -1

        val openLeft = same(c, left)
        val openRight = same(c, right)
        val openUp = same(c, up)
        val openDown = same(c, down)

        val lx = if (openLeft) x0 else x0 + inset
        val rx = if (openRight) x1 else x1 - inset
        val ty = if (openUp) y0 else y0 + inset
        val by = if (openDown) y1 else y1 - inset

        if (!openUp) drawSegment(Offset(lx, y0 + inset), Offset(rx, y0 + inset), stroke)
        if (!openDown) drawSegment(Offset(lx, y1 - inset), Offset(rx, y1 - inset), stroke)
        if (!openLeft) drawSegment(Offset(x0 + inset, ty), Offset(x0 + inset, by), stroke)
        if (!openRight) drawSegment(Offset(x1 - inset, ty), Offset(x1 - inset, by), stroke)
    }

    // Somme affichée dans la case la plus haute puis la plus à gauche de la cage.
    state.puzzle.cages.forEach { cage ->
        val anchor = cage.anchor
        val x = colOf(anchor) * cell + inset + 2f * density
        val y = rowOf(anchor) * cell + inset + paints.cageSum.textSize
        drawContext.canvas.nativeCanvas.drawText(cage.sum.toString(), x, y, paints.cageSum)
    }
}

private fun DrawScope.drawSegment(from: Offset, to: Offset, stroke: Stroke) {
    drawLine(
        color = Palette.CageDash,
        start = from,
        end = to,
        strokeWidth = stroke.width,
        pathEffect = stroke.pathEffect
    )
}

// ---- Chiffres et notes ----

private fun DrawScope.drawContents(state: BoardState, cell: Float, paints: BoardPaints) {
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
            if (notes != 0) drawNotes(canvas, notes, colOf(c) * cell, rowOf(c) * cell, cell, paints)
        }
    }
}

private fun drawNotes(
    canvas: android.graphics.Canvas,
    notes: Int,
    left: Float,
    top: Float,
    cell: Float,
    paints: BoardPaints
) {
    val step = cell / 3f
    paints.note.textSize = cell * 0.26f
    for (d in 1..9) {
        if (!maskContains(notes, d)) continue
        val i = d - 1
        val x = left + (i % 3) * step + step / 2f
        val y = top + (i / 3) * step + step / 2f
        canvas.drawText(
            d.toString(), x,
            y - (paints.note.ascent() + paints.note.descent()) / 2f,
            paints.note
        )
    }
}

/** Peintures natives réutilisées d'une frame à l'autre : mesurer 81 textes coûte. */
private class BoardPaints(density: Float) {
    private fun basePaint(color: Color, style: Int) = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        this.color = color.toArgb()
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
    }

    val given = basePaint(Palette.Given, Typeface.BOLD)
    val entry = basePaint(Palette.Entry, Typeface.NORMAL)
    val wrong = basePaint(Palette.Error, Typeface.BOLD)
    val revealed = basePaint(Palette.Success, Typeface.NORMAL)
    val note = basePaint(Palette.Note, Typeface.NORMAL)
    val cageSum = basePaint(Palette.CageSum, Typeface.BOLD).apply {
        textAlign = Paint.Align.LEFT
        textSize = 10f * density
    }
}
