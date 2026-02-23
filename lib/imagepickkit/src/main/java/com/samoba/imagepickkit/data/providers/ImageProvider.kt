package com.samoba.imagepickkit.data.providers

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.samoba.imagepickkit.domain.Image
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

/**
 * Provider for loading images from the device's MediaStore.
 * 
 * @param app Application context for content resolver access
 * @param supportedMimeTypes List of MIME types to include (e.g., "image/jpeg", "image/png")
 */
class ImageProvider(
    private val app: Application,
    private val supportedMimeTypes: Array<String> = DEFAULT_MIME_TYPES
) : MediaProvider<Image>() {

    // Cache for folder paths to avoid creating duplicate String objects
    private val pathCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val projection: Array<String> by lazy {
        arrayOf(
            // 0
            MediaStore.Files.FileColumns._ID,
            // 1
            MediaStore.Files.FileColumns.DATA,
            // 2
            MediaStore.Files.FileColumns.SIZE,
            // 3
            MediaStore.Files.FileColumns.WIDTH,
            // 4
            MediaStore.Files.FileColumns.HEIGHT,
            // 5
            MediaStore.Files.FileColumns.ORIENTATION,
            // 6
            MediaStore.Files.FileColumns.TITLE,
            // 7
            MediaStore.Files.FileColumns.DATE_ADDED,
            // 8
            MediaStore.Files.FileColumns.BUCKET_ID,
        )
    }

    private fun converter(cursor: Cursor): Image = with(cursor) {
        val orientation = getIntOrNull(5) ?: 0
        val width = when (orientation) {
            90, 270 -> getIntOrNull(4) ?: 0
            else -> getIntOrNull(3) ?: 0
        }
        val height = when (orientation) {
            90, 270 -> getIntOrNull(3) ?: 0
            else -> getIntOrNull(4) ?: 0
        }
        val path = getString(1)

        val parentPath = path.substringBeforeLast(File.separator)
        // Intern the parent path string to reduce memory footprint
        val cachedParent = pathCache.getOrPut(parentPath) { parentPath }

        Image(
            name = path.substringAfterLast(File.separator),
            parent = cachedParent,
            size = getLong(2),
            width = width,
            height = height,
            title = getStringOrNull(6) ?: "Unknown",
            dateAdded = Instant.ofEpochSecond(getLong(7))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime(),
            bucketId = getLong(8),
            mediaId = getLong(0),
            path = path,
        )
    }

    /**
     * Paginated query - only fetches the images needed for the current page.
     * Uses LIMIT and OFFSET for efficient database-level pagination.
     */
    override suspend fun getPage(
        offset: Int,
        limit: Int,
        dispatcher: CoroutineDispatcher
    ): List<Image> = withContext(dispatcher) {
        queryPaginated(offset, limit)
    }

    /**
     * Paginated query filtered by folder - applies folder filter at database level
     * for maximum efficiency.
     */
    suspend fun getPageByFolder(
        folderId: Long,
        offset: Int,
        limit: Int,
        dispatcher: CoroutineDispatcher
    ): List<Image> = withContext(dispatcher) {
        queryPaginated(
            offset = offset,
            limit = limit,
            selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ?",
            selectionArgs = arrayOf(folderId.toString())
        )
    }

    /**
     * Optimized query to get images in a folder with a hard limit.
     */
    suspend fun getImagesInFolderLimited(
        folderId: Long,
        limit: Int
    ): List<Image> = withContext(Dispatchers.IO) {
        queryPaginated(
            offset = 0,
            limit = limit,
            selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ?",
            selectionArgs = arrayOf(folderId.toString())
        )
    }

    /**
     * Optimized query to get all images with a hard limit.
     */
    suspend fun getAllImagesLimited(limit: Int): List<Image> = withContext(Dispatchers.IO) {
        queryPaginated(offset = 0, limit = limit)
    }

    private fun queryPaginated(
        offset: Int,
        limit: Int,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<Image> {
        val query = queryWithPagination(
            offset = offset,
            limit = limit,
            selection = selection,
            selectionArgs = selectionArgs
        ) ?: return emptyList()

        return buildList {
            query.use { cursor ->
                while (cursor.moveToNext()) {
                    add(converter(cursor))
                }
            }
        }
    }

    private fun getAllImages(): List<Image> {
        val query = query() ?: return emptyList()

        return buildList {
            query.use { cursor ->
                while (cursor.moveToNext()) {
                    add(converter(cursor))
                }
            }
        }
    }

    /**
     * Optimized query to get only IDs - much faster than full Image objects.
     */
    fun getIdsOnly(limit: Int? = null): Set<Long> {
        val idProjection = arrayOf(MediaStore.Files.FileColumns._ID)
        val query = if (limit != null) {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, generateMimeTypeSelection())
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, supportedMimeTypes)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Images.Media.DATE_ADDED} DESC")
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            }
            app.contentResolver.query(MediaStore.Files.getContentUri("external"), idProjection, queryArgs, null)
        } else {
            query(projection = idProjection)
        } ?: return emptySet()

        return buildSet {
            query.use { cursor ->
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0))
                }
            }
        }
    }

    /**
     * Highly optimized SQL COUNT query.
     */
    fun getCount(): Int {
        val query = app.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns._ID),
            generateMimeTypeSelection(),
            supportedMimeTypes,
            null
        ) ?: return 0
        return query.use { it.count }
    }

    private fun generateMimeTypeSelection(): String {
        return supportedMimeTypes.joinToString(" OR ") { "${MediaStore.Files.FileColumns.MIME_TYPE} = ?" }
    }

    override suspend fun refresh(context: CoroutineContext): Deferred<List<Image>> =
        coroutineScope {
            val job = async(context) { getAllImages() }
            media = job
            job
        }

    fun getByIds(ids: List<Long>): List<Image> {
        val query = query(
            selection = "${MediaStore.Images.Media._ID} IN (${ids.joinToString { "?" }})",
            selectionArgs = ids.map { it.toString() }.toTypedArray()
        ) ?: return emptyList()

        return buildList {
            query.use { cursor ->
                while (cursor.moveToNext()) {
                    add(converter(cursor))
                }
            }
        }
    }

    fun getById(id: Long): Image {
        val query = query(
            selection = "${MediaStore.Images.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString())
        )

        if (query == null || !query.moveToFirst()) {
            throw Exception("Image not found")
        } else {
            return query.use { converter(it) }
        }
    }

    /**
     * Paginated query using Bundle for LIMIT/OFFSET (Android O+)
     */
    private fun queryWithPagination(
        offset: Int,
        limit: Int,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        projection: Array<String>? = null,
    ): Cursor? {
        val collectionUri = MediaStore.Files.getContentUri("external")
        val mimeTypeSelection = generateMimeTypeSelection()

        val finalSelection = if (selection != null) {
            "($mimeTypeSelection) AND ($selection)"
        } else {
            mimeTypeSelection
        }

        val finalSelectionArgs = if (selectionArgs != null) {
            supportedMimeTypes + selectionArgs
        } else {
            supportedMimeTypes
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, finalSelection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, finalSelectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Images.Media.DATE_ADDED} DESC")
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            app.contentResolver.query(collectionUri, projection ?: this.projection, queryArgs, null)
        } else {
            app.contentResolver.query(
                collectionUri,
                projection ?: this.projection,
                finalSelection,
                finalSelectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
            )
        }
    }

    private fun query(
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): Cursor? {
        val collectionUri = MediaStore.Files.getContentUri("external")

        val mimeTypeSelection = generateMimeTypeSelection()

        val finalSelection = if (selection != null) {
            "($mimeTypeSelection) AND ($selection)"
        } else {
            mimeTypeSelection
        }

        val finalSelectionArgs = if (selectionArgs != null) {
            supportedMimeTypes + selectionArgs
        } else {
            supportedMimeTypes
        }

        return app.contentResolver.query(
            collectionUri,
            projection ?: this.projection,
            finalSelection,
            finalSelectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
    }

    companion object {
        val DEFAULT_MIME_TYPES = arrayOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",
            "image/heic",
            "image/heif"
        )
    }
}
