package com.samoba.imagepickkit.data.providers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base provider class for media items with caching and pagination support.
 */
abstract class MediaProvider<T> {
    protected var media: Deferred<List<T>>? = null

    suspend fun getOrRefresh(
        context: CoroutineContext = EmptyCoroutineContext,
    ): List<T> = media?.await() ?: refresh(context).await()

    suspend fun isEmpty(): Boolean = media?.await()?.isEmpty() ?: true

    open fun invalidateCache() {
        media = null
    }

    abstract suspend fun getPage(
        offset: Int,
        limit: Int,
        dispatcher: CoroutineDispatcher
    ): List<T>

    abstract suspend fun refresh(
        context: CoroutineContext = EmptyCoroutineContext,
    ): Deferred<List<T>>
}
