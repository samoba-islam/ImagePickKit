package com.samoba.imagepickkit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A circular check indicator for selection state.
 */
@Composable
internal fun CheckCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
    selectedBrush: Brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    ),
    unselectedColor: Color = Color.White.copy(alpha = 0.7f)
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else Color.Transparent,
        animationSpec = tween(150),
        label = "check_bg"
    )

    Box(
        modifier = modifier
            .padding(6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(selectedBrush, CircleShape)
                } else {
                    Modifier
                        .background(backgroundColor, CircleShape)
                        .border(2.dp, unselectedColor, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
