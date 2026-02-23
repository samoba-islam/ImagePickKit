package com.samoba.imagepickkit.ui

import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.samoba.imagepickkit.domain.Folder
import com.samoba.imagepickkit.domain.FolderImages
import com.samoba.imagepickkit.domain.Image
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow

/**
 * UI state for the image picker screen.
 */
@Stable
internal data class ImagePickerState(
    val images: Flow<PagingData<Image>>? = null,
    val folderImages: FolderImages? = null,
    val folders: Flow<PagingData<Folder>>? = null,
    val selectedImages: ImmutableList<Image> = persistentListOf(),
    val selectedImageIds: ImmutableSet<Long> = persistentSetOf(),
    val isSelectedAll: Boolean = false,
    val currentFolderSelected: Boolean = false,
    val allPhotosIds: ImmutableSet<Long> = persistentSetOf(),
    val currentFolderImageIds: ImmutableSet<Long> = persistentSetOf(),
    val message: String? = null,
    val maxSelectionReached: Boolean = false
)
