package com.atelbay.money_manager.core.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class CircleRevealShape(
    private val radius: Float,
    private val inverted: Boolean = false,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline.Generic {
        val circlePath = Path().apply {
            addOval(
                Rect(
                    center = Offset(size.width / 2, size.height / 2),
                    radius = radius,
                ),
            )
        }
        return if (inverted) {
            val fullRect = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            Outline.Generic(
                Path().apply {
                    op(fullRect, circlePath, PathOperation.Difference)
                },
            )
        } else {
            Outline.Generic(circlePath)
        }
    }
}
