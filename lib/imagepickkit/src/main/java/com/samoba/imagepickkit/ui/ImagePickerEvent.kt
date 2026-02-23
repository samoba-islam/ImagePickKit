package com.samoba.imagepickkit.ui

import androidx.compose.runtime.Immutable
import com.samoba.imagepickkit.domain.Folder
import com.samoba.imagepickkit.domain.Image

/**
 * Events that can occur in the image picker.
 */
@Immutable
internal sealed interface ImagePickerEvent {
    data class ImageClicked(val image: Image) : ImagePickerEvent
    data class FolderClicked(val folder: Folder?) : ImagePickerEvent
    data object ClearSelectedImages : ImagePickerEvent
    data class SelectAll(val folderId: Long? = null) : ImagePickerEvent
}
