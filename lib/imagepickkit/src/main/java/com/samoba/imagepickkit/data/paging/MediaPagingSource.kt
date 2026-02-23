package com.samoba.imagepickkit.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.samoba.imagepickkit.data.providers.MediaProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optimized paging source that uses database-level pagination.
 * Instead of loading all items first and paginating in memory,
 * this delegates proper LIMIT/OFFSET queries to the provider.
 */
abstract class MediaPagingSource<T : Any>(
    private val mediaProvider: MediaProvider<T>,
    private val appDispatchers: CoroutineDispatcher = Dispatchers.IO,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        withContext(appDispatchers) {
            try {
                val startIndex = params.key ?: 0
                val loadSize = params.loadSize

                // Use paginated query from provider
                val result = mediaProvider.getPage(
                    offset = startIndex,
                    limit = loadSize,
                    dispatcher = appDispatchers
                )

                // Apply any subclass filtering
                val filteredResult = filter(result)

                val hasMore = result.size >= loadSize

                LoadResult.Page(
                    data = filteredResult,
                    nextKey = if (hasMore) startIndex + result.size else null,
                    prevKey = null
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

    open fun filter(list: List<T>): List<T> = list
}
