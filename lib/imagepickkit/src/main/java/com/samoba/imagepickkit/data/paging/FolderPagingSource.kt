package com.samoba.imagepickkit.data.paging

import com.samoba.imagepickkit.data.providers.FolderProvider
import com.samoba.imagepickkit.domain.Folder

/**
 * Paging source for loading folders.
 */
class FolderPagingSource(
    folderProvider: FolderProvider
) : MediaPagingSource<Folder>(folderProvider) {

    fun interface Factory {
        fun create(): FolderPagingSource
    }
}
