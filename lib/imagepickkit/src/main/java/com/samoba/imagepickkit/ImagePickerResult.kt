package com.samoba.imagepickkit

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Result from the image picker.
 */
@Immutable
sealed interface ImagePickerResult {
    /**
     * User successfully selected images.
     */
    data class Success(val images: List<SelectedImage>) : ImagePickerResult
    
    /**
     * User cancelled the selection.
     */
    data object Cancelled : ImagePickerResult
}

/**
 * Represents a selected image with its metadata.
 */
@Immutable
data class SelectedImage(
    /**
     * Content URI of the image.
     */
    val uri: Uri,
    
    /**
     * File name of the image.
     */
    val name: String,
    
    /**
     * File size in bytes.
     */
    val size: Long,
    
    /**
     * Image width in pixels.
     */
    val width: Int,
    
    /**
     * Image height in pixels.
     */
    val height: Int,
    
    /**
     * MediaStore media ID.
     */
    val mediaId: Long
)
