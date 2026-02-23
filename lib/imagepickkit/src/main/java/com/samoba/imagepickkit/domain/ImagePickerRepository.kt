package com.samoba.imagepickkit.domain

import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing images and folders from the device.
 */
interface ImagePickerRepository {

    fun foldersPagingDataFlow(): Flow<PagingData<Folder>>

    fun imagesInFolderPagingDataFlow(
        folderId: Long? = null
    ): Flow<PagingData<Image>>

    fun imagesByIdPagingDataFlow(
        ids: List<Long>
    ): Flow<PagingData<Image>>

    fun getImagesByIdFlow(ids: List<Long>): Flow<ImmutableList<Image>>
    suspend fun getImagesById(ids: List<Long>): List<Image>

    suspend fun getImagesByUris(uris: List<String>): List<Image>

    fun getImageById(id: Long): Flow<Image>

    suspend fun getAllImages(limit: Int? = null): List<Image>
    suspend fun getAllImageIds(limit: Int? = null): Set<Long>

    suspend fun getImagesInFolder(folderId: Long, limit: Int? = null): List<Image>

    suspend fun imagesSize(): Int
    suspend fun imagesCount(): Int

    fun refreshData()
}
