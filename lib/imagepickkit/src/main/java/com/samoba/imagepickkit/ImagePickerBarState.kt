package com.samoba.imagepickkit

import androidx.compose.runtime.Immutable

/**
 * State exposed to custom TopBar and BottomBar composables.
 * Contains information about the current selection state.
 */
@Immutable
data class ImagePickerBarState(
    /**
     * Number of currently selected images.
     */
    val selectedCount: Int,
    
    /**
     * List of currently selected images with their metadata.
     */
    val selectedImages: List<SelectedImage>,
    
    /**
     * Whether the "Select All" checkbox is currently checked.
     * True if all images in the current view (photos tab or folder) are selected.
     */
    val isSelectAllChecked: Boolean,
    
    /**
     * Whether the maximum selection limit has been reached.
     */
    val maxSelectionReached: Boolean
)
