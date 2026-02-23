package com.samoba.imagepickkit

import androidx.compose.runtime.Composable

/**
 * Configuration options for the image picker.
 */
data class ImagePickerConfig(
    /**
     * Maximum number of images that can be selected.
     * Default is [Int.MAX_VALUE] for unlimited selection.
     */
    val maxSelection: Int = Int.MAX_VALUE,
    
    /**
     * List of MIME types to show in the picker.
     * Default includes common image formats.
     */
    val mimeTypes: List<String> = listOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/avif",
        "image/heic",
        "image/heif"
    ),
    
    /**
     * Number of columns in the photo grid.
     * Default is 3.
     */
    val gridColumns: Int = 3,
    
    /**
     * Text for the continue/done button.
     * Default is "Continue".
     */
    val continueButtonText: String = "Continue",
    
    /**
     * Text for the Photos tab.
     * Default is "Photos".
     */
    val photosTabText: String = "Photos",
    
    /**
     * Text for the Folders tab.
     * Default is "Folders".
     */
    val foldersTabText: String = "Folders",
    
    /**
     * Title for the image picker screen.
     * Default is "Select Images".
     */
    val title: String = "Select Images",

    /**
     * Whether to show the "Select All" option in the top bar.
     * Default is true.
     */
    val showSelectAll: Boolean = true,

    /**
     * List of URIs to pre-select when the picker opens.
     */
    val selectedUris: List<String> = emptyList(),

    /**
     * Custom composable for the TopBar.
     * If null, the default TopBar will be used.
     * 
     * @param state Current selection state
     * @param onBackClick Callback to navigate back
     * @param onSelectAllClick Callback to toggle select all
     * @param onClearSelectionClick Callback to clear all selections
     */
    val topBar: (@Composable (
        state: ImagePickerBarState,
        onBackClick: () -> Unit,
        onSelectAllClick: () -> Unit,
        onClearSelectionClick: () -> Unit
    ) -> Unit)? = null,

    /**
     * Custom composable for the BottomBar.
     * If null, the default BottomBar will be used.
     * 
     * @param state Current selection state
     * @param onContinueClick Callback when continue button is clicked
     */
    val bottomBar: (@Composable (
        state: ImagePickerBarState,
        onContinueClick: () -> Unit
    ) -> Unit)? = null
)

