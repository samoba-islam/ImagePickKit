package com.samoba.imagepickkit.domain

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * Represents the images within a specific folder.
 */
@Immutable
data class FolderImages(
    val images: Flow<PagingData<Image>>,
    val folder: Folder
)
