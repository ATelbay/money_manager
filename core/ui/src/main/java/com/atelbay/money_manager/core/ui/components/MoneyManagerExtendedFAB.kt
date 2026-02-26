package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

/**
 * Custom Extended FAB for Money Manager with gradient background and spring animations.
 */
@Composable
fun MoneyManagerExtendedFAB(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    expanded: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
        label = "fabScale",
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Teal,
            Color(0xFF00B396) // Slightly darker teal
        )
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .background(gradientBrush)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(visible = expanded) {
                Row {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MoneyManagerTheme.typography.button,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MoneyManagerExtendedFABPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerExtendedFAB(
            onClick = {},
            text = "Добавить",
        )
    }
}

@Preview
@Composable
private fun MoneyManagerExtendedFABCollapsedPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerExtendedFAB(
            onClick = {},
            text = "Добавить",
            expanded = false,
        )
    }
}
