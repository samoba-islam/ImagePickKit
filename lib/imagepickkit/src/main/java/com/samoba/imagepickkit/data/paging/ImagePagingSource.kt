package com.samoba.imagepickkit.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.samoba.imagepickkit.data.providers.ImageProvider
import com.samoba.imagepickkit.domain.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optimized image paging source that uses database-level filtering and pagination.
 * When a folderId is specified, filtering happens at the SQL level for maximum performance.
 */
class ImagePagingSource(
    private val folderId: Long?,
    private val ids: List<Long>?,
    private val imageProvider: ImageProvider
) : PagingSource<Int, Image>() {

    override fun getRefreshKey(state: PagingState<Int, Image>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Image> =
        withContext(Dispatchers.IO) {
            try {
                val startIndex = params.key ?: 0
                val loadSize = params.loadSize

                val result = when {
                    // Folder filtering at database level for best performance
                    folderId != null -> imageProvider.getPageByFolder(
                        folderId = folderId,
                        offset = startIndex,
                        limit = loadSize,
                        dispatcher = Dispatchers.IO
                    )
                    // ID filtering - less common, filter in memory
                    ids != null -> {
                        val page = imageProvider.getPage(startIndex, loadSize, Dispatchers.IO)
                        page.filter { it.mediaId in ids }
                    }
                    // All images - direct pagination
                    else -> imageProvider.getPage(startIndex, loadSize, Dispatchers.IO)
                }

                val hasMore = result.size >= loadSize

                LoadResult.Page(
                    data = result,
                    nextKey = if (hasMore) startIndex + result.size else null,
                    prevKey = null
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

    fun interface Factory {
        fun create(folderId: Long?, ids: List<Long>?): ImagePagingSource
    }
}
