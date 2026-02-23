package com.samoba.imagepickkit.domain

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

/**
 * Represents an image from the device's media store.
 */
@Immutable
data class Image(
    val name: String = "",
    val parent: String = "",
    val title: String = "",
    val path: String = "",
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mediaId: Long = 0L,
    val bucketId: Long = 0L,
    val dateAdded: LocalDateTime
) {
    val resolution: String get() = "${width}x$height"
    val aspectRatio: Float get() = if (height > 0) width / height.toFloat() else 1f
}

/**
 * Extension to convert an Image to its content URI.
 */
fun Image.toUri(): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        mediaId
    )
}
