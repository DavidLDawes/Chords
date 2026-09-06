package com.virtualsoundnw.chords.ui.fretboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualsoundnw.chords.theme.ChordsTheme
import com.virtualsoundnw.chords.voicing.GuitarVoicing

private const val STRING_COUNT = GuitarVoicing.STRING_COUNT
private val TOP_MARGIN = 28.dp // room for the X/O markers above the nut
private val SIDE_MARGIN = 24.dp // room for the fret-number label when the nut isn't shown

/** Draws [voicing] as a standard chord-chart diagram: strings, frets, and fingering dots. */
@Composable
fun FretboardDiagramView(voicing: GuitarVoicing, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary
    val labelStyle = TextStyle(color = lineColor, fontSize = 16.sp, textAlign = TextAlign.Center)

    val baseFret = FretboardLayout.baseFret(voicing.frets)

    Canvas(modifier = modifier.aspectRatio(STRING_COUNT.toFloat() / (FretboardLayout.FRET_WINDOW_SIZE + 1))) {
        val gridLeft = SIDE_MARGIN.toPx()
        val gridTop = TOP_MARGIN.toPx()
        val gridWidth = size.width - gridLeft
        val gridHeight = size.height - gridTop
        val stringSpacing = gridWidth / (STRING_COUNT - 1)
        val fretHeight = gridHeight / FretboardLayout.FRET_WINDOW_SIZE
        val stringX = List(STRING_COUNT) { gridLeft + it * stringSpacing }

        drawStrings(stringX, gridTop, gridHeight, lineColor)
        drawFretLines(gridLeft, gridTop, gridWidth, fretHeight, baseFret, lineColor)
        if (baseFret > 1) {
            drawFretNumberLabel(textMeasurer, labelStyle, baseFret, gridLeft, gridTop, fretHeight)
        }
        voicing.frets.forEachIndexed { stringIndex, fret ->
            drawStringMarker(
                marker = FretboardLayout.markerFor(fret, baseFret),
                x = stringX[stringIndex],
                gridTop = gridTop,
                fretHeight = fretHeight,
                stringSpacing = stringSpacing,
                dotColor = dotColor,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
            )
        }
    }
}

private fun DrawScope.drawStrings(stringX: List<Float>, gridTop: Float, gridHeight: Float, color: Color) {
    stringX.forEach { x ->
        drawLine(color = color, start = Offset(x, gridTop), end = Offset(x, gridTop + gridHeight), strokeWidth = 2f)
    }
}

private fun DrawScope.drawFretLines(
    gridLeft: Float,
    gridTop: Float,
    gridWidth: Float,
    fretHeight: Float,
    baseFret: Int,
    color: Color,
) {
    val nutStrokeWidth = 8f
    val fretStrokeWidth = 2f
    for (row in 0..FretboardLayout.FRET_WINDOW_SIZE) {
        val y = gridTop + row * fretHeight
        val isNut = row == 0 && baseFret == 1
        drawLine(
            color = color,
            start = Offset(gridLeft, y),
            end = Offset(gridLeft + gridWidth, y),
            strokeWidth = if (isNut) nutStrokeWidth else fretStrokeWidth,
        )
    }
}

private fun DrawScope.drawFretNumberLabel(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    baseFret: Int,
    gridLeft: Float,
    gridTop: Float,
    fretHeight: Float,
) {
    val text = "${baseFret}fr"
    val layout = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(gridLeft - layout.size.width - 4.dp.toPx(), gridTop + fretHeight / 2 - layout.size.height / 2),
    )
}

private fun DrawScope.drawStringMarker(
    marker: FretboardLayout.StringMarker,
    x: Float,
    gridTop: Float,
    fretHeight: Float,
    stringSpacing: Float,
    dotColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    when (marker) {
        is FretboardLayout.StringMarker.Fretted -> {
            val centerY = gridTop + (marker.row + 0.5f) * fretHeight
            drawCircle(color = dotColor, radius = stringSpacing * 0.32f, center = Offset(x, centerY))
        }
        FretboardLayout.StringMarker.Muted, FretboardLayout.StringMarker.Open -> {
            val text = if (marker == FretboardLayout.StringMarker.Muted) "X" else "O"
            val layout = textMeasurer.measure(text, labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x - layout.size.width / 2, gridTop - layout.size.height - 6.dp.toPx()),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun FretboardDiagramViewOpenChordPreview() {
    ChordsTheme {
        // Am7: X 0 2 0 1 0
        FretboardDiagramView(GuitarVoicing(listOf(null, 0, 2, 0, 1, 0)))
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun FretboardDiagramViewBarreChordPreview() {
    ChordsTheme {
        // D#7 barre shape starting at fret 6: X 6 8 6 8 6
        FretboardDiagramView(GuitarVoicing(listOf(null, 6, 8, 6, 8, 6)))
    }
}
