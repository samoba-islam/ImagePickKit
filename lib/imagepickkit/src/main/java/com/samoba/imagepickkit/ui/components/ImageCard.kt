package com.samoba.imagepickkit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.samoba.imagepickkit.domain.Image
import com.samoba.imagepickkit.domain.toUri

private const val THUMBNAIL_SIZE = 256

/**
 * A card displaying an image thumbnail with selection state.
 */
@Composable
internal fun ImageCard(
    image: Image,
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (Image) -> Unit
) {
    val context = LocalContext.current
    val formattedSize = remember(image.size) { formatFileSize(image.size) }
    val resolution = remember(image.width, image.height) { image.resolution }
    val imageUri = remember(image.mediaId) { image.toUri() }

    // Check memory cache synchronously to avoid flickering
    val initialBitmap = remember(imageUri) {
        ThumbnailCache.getFromMemoryCache(imageUri, THUMBNAIL_SIZE)
    }

    val bitmapState = produceState(initialValue = initialBitmap, key1 = imageUri) {
        if (value == null) {
            value = ThumbnailCache.getThumbnail(
                context = context,
                uri = imageUri,
                maxSize = THUMBNAIL_SIZE
            )
        }
    }

    val bitmap = bitmapState.value
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    // Simple alpha animation
    val alpha by animateFloatAsState(
        targetValue = if (imageBitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "image_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1F)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .clickable { onClick(image) }
            .testTag("image_card"),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (imageBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        }

        // Overlay and Labels
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(0.1F))
        ) {
            CheckCircle(
                selected = selected,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.scrim.copy(0.3F))
                    .padding(start = 6.dp, end = 2.dp, bottom = 5.dp, top = 6.dp)
            ) {
                Text(
                    text = formattedSize,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = resolution,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Format bytes as human-readable file size.
 */
internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
