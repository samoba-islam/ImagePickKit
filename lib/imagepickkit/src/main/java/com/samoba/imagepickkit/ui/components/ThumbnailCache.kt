package com.samoba.imagepickkit.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LRU cache for thumbnail bitmaps with AVIF/HEIF software acceleration support.
 * Uses HeifCoder as a fallback for devices without hardware codec support.
 */
object ThumbnailCache {
    
    private const val TAG = "ThumbnailCache"
    private const val DEFAULT_MAX_SIZE = 256
    
    // MIME types that may need software decoding
    private val HEIF_MIME_TYPES = setOf(
        "image/heic", "image/heif", "image/avif",
        "image/heic-sequence", "image/heif-sequence"
    )
    
    // Use 1/8th of available memory for cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    
    // Lazy-initialized HeifCoder instance
    private val heifCoder by lazy { HeifCoder() }
    
    /**
     * Get a cached thumbnail synchronously.
     * Returns null if not in cache.
     */
    fun getFromMemoryCache(uri: Uri, maxSize: Int): Bitmap? {
        val key = getCacheKey(uri, maxSize)
        return memoryCache.get(key)
    }
    
    /**
     * Load a thumbnail, using cache if available.
     * Falls back to HeifCoder for AVIF/HEIF if system decoding fails.
     */
    suspend fun getThumbnail(
        context: Context,
        uri: Uri,
        maxSize: Int = DEFAULT_MAX_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = getCacheKey(uri, maxSize)
        
        // Check cache first
        memoryCache.get(key)?.let { return@withContext it }
        
        // Try system thumbnail loading first
        var bitmap = loadSystemThumbnail(context, uri, maxSize)
        
        // If system loading failed, try software decoding with HeifCoder
        if (bitmap == null) {
            bitmap = loadWithHeifCoder(context, uri, maxSize)
        }
        
        bitmap?.let { memoryCache.put(key, it) }
        bitmap
    }
    
    /**
     * Load thumbnail using system APIs (hardware accelerated when available).
     */
    private fun loadSystemThumbnail(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(
                    uri,
                    Size(maxSize, maxSize),
                    null
                )
            } catch (e: Exception) {
                Log.d(TAG, "System thumbnail failed for $uri: ${e.message}")
                null
            }
        } else {
            try {
                val mediaId = uri.lastPathSegment?.toLongOrNull() ?: return null
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    mediaId,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            } catch (e: Exception) {
                Log.d(TAG, "Legacy thumbnail failed for $uri: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Load and decode using HeifCoder (software-based AVIF/HEIF decoder).
     * This provides software acceleration for devices without hardware codec support.
     */
    private fun loadWithHeifCoder(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        return try {
            // Read bytes from the content URI
            val bytes = context.contentResolver.openInputStream(uri)?.use { 
                it.readBytes() 
            } ?: return null
            
            // First try standard BitmapFactory (for non-HEIF formats)
            val standardBitmap = tryStandardDecode(bytes, maxSize)
            if (standardBitmap != null) {
                return standardBitmap
            }
            
            // Try HeifCoder for AVIF/HEIF software decoding
            Log.d(TAG, "Attempting HeifCoder software decode for $uri")
            val decoded = heifCoder.decode(bytes)
            
            // Scale down if needed
            val scaledBitmap = scaleBitmapToMaxSize(decoded, maxSize)
            
            // Recycle original if we created a scaled copy
            if (scaledBitmap !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
            
            Log.d(TAG, "HeifCoder successfully decoded $uri")
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "HeifCoder failed for $uri: ${e.message}")
            null
        }
    }
    
    /**
     * Try standard BitmapFactory decoding with sample size calculation.
     */
    private fun tryStandardDecode(bytes: ByteArray, maxSize: Int): Bitmap? {
        return try {
            // First, decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(
                options.outWidth, 
                options.outHeight, 
                maxSize, 
                maxSize
            )
            options.inJustDecodeBounds = false
            
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Scale bitmap to fit within maxSize while maintaining aspect ratio.
     */
    private fun scaleBitmapToMaxSize(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Calculate optimal sample size for decoding.
     */
    private fun calculateInSampleSize(
        width: Int, 
        height: Int, 
        reqWidth: Int, 
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    private fun getCacheKey(uri: Uri, maxSize: Int): String {
        return "${uri}_$maxSize"
    }
    
    /**
     * Clear the entire cache.
     */
    fun clearCache() {
        memoryCache.evictAll()
    }
}
