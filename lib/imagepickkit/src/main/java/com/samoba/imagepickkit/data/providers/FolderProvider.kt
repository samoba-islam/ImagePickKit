package com.samoba.imagepickkit.data.providers

import com.samoba.imagepickkit.domain.Folder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Provider for loading folders (buckets) from the device's MediaStore.
 * Folders are derived by grouping images by their bucket ID.
 */
class FolderProvider(
    private val imageProvider: ImageProvider
) : MediaProvider<Folder>() {

    // Cache for folders - folders are typically a small list
    private var cachedFolders: List<Folder>? = null

    override suspend fun getPage(
        offset: Int,
        limit: Int,
        dispatcher: CoroutineDispatcher
    ): List<Folder> = withContext(dispatcher) {
        val folders = cachedFolders ?: getAllFolders(dispatcher).also { cachedFolders = it }
        val endIndex = minOf(offset + limit, folders.size)
        if (offset >= folders.size) emptyList() else folders.subList(offset, endIndex)
    }

    private suspend fun getAllFolders(context: CoroutineContext): List<Folder> {
        return imageProvider.getOrRefresh(context).groupBy { it.bucketId }.mapNotNull {
            val first = it.value.firstOrNull()

            if (first == null) {
                null
            } else {
                Folder(
                    path = first.parent,
                    name = first.parent.substringAfterLast(File.separator),
                    imagesCount = it.value.size,
                    imagesSize = it.value.sumOf { v -> v.size },
                    id = it.key,
                    lastAddedDate = it.value.maxBy { v -> v.dateAdded }.dateAdded
                )
            }
        }
    }

    override suspend fun refresh(context: CoroutineContext): Deferred<List<Folder>> =
        coroutineScope {
            val job = async(context) {
                getAllFolders(context).also { cachedFolders = it }
            }
            media = job
            job
        }

    override fun invalidateCache() {
        super.invalidateCache()
        cachedFolders = null
        imageProvider.invalidateCache()
    }
}
