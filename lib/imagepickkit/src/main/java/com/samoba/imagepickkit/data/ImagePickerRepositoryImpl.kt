package com.samoba.imagepickkit.data

import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.samoba.imagepickkit.data.paging.FolderPagingSource
import com.samoba.imagepickkit.data.paging.ImagePagingSource
import com.samoba.imagepickkit.data.providers.FolderProvider
import com.samoba.imagepickkit.data.providers.ImageProvider
import com.samoba.imagepickkit.domain.Folder
import com.samoba.imagepickkit.domain.Image
import com.samoba.imagepickkit.domain.ImagePickerRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of ImagePickerRepository using MediaStore.
 */
class ImagePickerRepositoryImpl(
    private val imagePagingSourceFactory: ImagePagingSource.Factory,
    private val folderPagingSourceFactory: FolderPagingSource.Factory,
    private val imageProvider: ImageProvider,
    private val folderProvider: FolderProvider,
    private val appDispatchers: CoroutineDispatcher = Dispatchers.IO
) : ImagePickerRepository {

    override fun foldersPagingDataFlow(): Flow<PagingData<Folder>> =
        Pager(
            PagingConfig(
                pageSize = 50,
                initialLoadSize = 50,
                enablePlaceholders = true,
                prefetchDistance = 35
            ),
            pagingSourceFactory = { folderPagingSourceFactory.create() }
        ).flow

    override fun imagesInFolderPagingDataFlow(
        folderId: Long?
    ): Flow<PagingData<Image>> =
        Pager(
            DefaultPagingConfig,
            pagingSourceFactory = {
                imagePagingSourceFactory.create(folderId, null)
            }
        ).flow

    override fun imagesByIdPagingDataFlow(ids: List<Long>): Flow<PagingData<Image>> =
        Pager(
            DefaultPagingConfig,
            pagingSourceFactory = {
                imagePagingSourceFactory.create(null, ids)
            }
        ).flow

    override fun getImagesByIdFlow(ids: List<Long>): Flow<ImmutableList<Image>> = flow {
        emit(getImagesById(ids).toPersistentList())
    }.flowOn(appDispatchers)

    override fun getImageById(id: Long): Flow<Image> = flow {
        emit(imageProvider.getById(id))
    }.flowOn(appDispatchers)

    override suspend fun getImagesById(ids: List<Long>): List<Image> =
        withContext(appDispatchers) {
            imageProvider.getByIds(ids)
        }

    override suspend fun getImagesByUris(uris: List<String>): List<Image> =
        withContext(appDispatchers) {
            val mediaIds = uris.mapNotNull { uriString ->
                try {
                    val uri = uriString.toUri()
                    android.content.ContentUris.parseId(uri)
                } catch (e: Exception) {
                    null
                }
            }
            if (mediaIds.isNotEmpty()) {
                imageProvider.getByIds(mediaIds)
            } else {
                emptyList()
            }
        }

    override suspend fun getAllImages(limit: Int?): List<Image> = withContext(appDispatchers) {
        if (limit != null) {
            imageProvider.getAllImagesLimited(limit)
        } else {
            imageProvider.getOrRefresh(appDispatchers)
        }
    }

    override suspend fun getAllImageIds(limit: Int?): Set<Long> = withContext(appDispatchers) {
        imageProvider.getIdsOnly(limit)
    }

    override suspend fun imagesSize(): Int = imagesCount()

    override suspend fun imagesCount(): Int = withContext(appDispatchers) {
        imageProvider.getCount()
    }

    override suspend fun getImagesInFolder(folderId: Long, limit: Int?): List<Image> =
        withContext(appDispatchers) {
            if (limit != null) {
                imageProvider.getImagesInFolderLimited(folderId, limit)
            } else {
                imageProvider.getOrRefresh(appDispatchers).filter { it.bucketId == folderId }
            }
        }

    override fun refreshData() {
        folderProvider.invalidateCache()
    }

    companion object {
        // Optimized paging config for smooth scrolling
        private val DefaultPagingConfig = PagingConfig(
            pageSize = 30,
            initialLoadSize = 30,
            enablePlaceholders = false,
            prefetchDistance = 15
        )
    }
}
