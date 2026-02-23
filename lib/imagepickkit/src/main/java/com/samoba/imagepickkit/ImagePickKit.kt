package com.samoba.imagepickkit

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samoba.imagepickkit.data.ImagePickerRepositoryImpl
import com.samoba.imagepickkit.data.paging.FolderPagingSource
import com.samoba.imagepickkit.data.paging.ImagePagingSource
import com.samoba.imagepickkit.data.providers.FolderProvider
import com.samoba.imagepickkit.data.providers.ImageProvider
import com.samoba.imagepickkit.domain.toUri
import com.samoba.imagepickkit.ui.ImagePickerScreenContent
import com.samoba.imagepickkit.ui.ImagePickerViewModel

/**
 * Main entry point for the image picker library.
 * 
 * ImagePickKit is a modern, high-performance Android image picker library built with Jetpack Compose 
 * and Material 3. Features multi-selection, folder browsing, and seamless paging for large libraries. 
 * Includes built-in support for AVIF/HEIF formats. Fast, lightweight, and fully customizable for a 
 * premium media selection experience in your Android apps.
 * 
 * This composable displays a full-screen image picker with:
 * - Grid view of all photos
 * - Folder browsing capability
 * - Multi-selection support
 * - Configurable selection limits
 * 
 * @param config Configuration options for the picker. See [ImagePickerConfig].
 * @param onResult Callback invoked when the user completes or cancels selection.
 * 
 * Example usage:
 * ```kotlin
 * ImagePickerScreen(
 *     config = ImagePickerConfig(maxSelection = 10),
 *     onResult = { result ->
 *         when (result) {
 *             is ImagePickerResult.Success -> {
 *                 // Handle selected images
 *                 result.images.forEach { image ->
 *                     println("Selected: ${image.uri}")
 *                 }
 *             }
 *             ImagePickerResult.Cancelled -> {
 *                 // User cancelled
 *             }
 *         }
 *     }
 * )
 * ```
 */
@Composable
fun ImagePickerScreen(
    config: ImagePickerConfig = ImagePickerConfig(),
    onResult: (ImagePickerResult) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Create dependencies
    val imageProvider = remember(config.mimeTypes) {
        ImageProvider(application, config.mimeTypes.toTypedArray())
    }
    val folderProvider = remember(imageProvider) { FolderProvider(imageProvider) }
    
    val imagePagingSourceFactory = remember(imageProvider) {
        ImagePagingSource.Factory { folderId, ids ->
            ImagePagingSource(folderId, ids, imageProvider)
        }
    }
    val folderPagingSourceFactory = remember(folderProvider) {
        FolderPagingSource.Factory { FolderPagingSource(folderProvider) }
    }
    
    val repository = remember(imagePagingSourceFactory, folderPagingSourceFactory) {
        ImagePickerRepositoryImpl(
            imagePagingSourceFactory = imagePagingSourceFactory,
            folderPagingSourceFactory = folderPagingSourceFactory,
            imageProvider = imageProvider,
            folderProvider = folderProvider
        )
    }

    val sessionID = remember { java.util.UUID.randomUUID().toString() }

    val viewModel: ImagePickerViewModel = viewModel(
        key = "image_picker_${config.hashCode()}_$sessionID",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ImagePickerViewModel(repository, config) as T
            }
        }
    )

    ImagePickerScreenContent(
        viewModel = viewModel,
        config = config,
        onBackClick = {
            onResult(ImagePickerResult.Cancelled)
        },
        onContinueClick = {
            val selectedImages = viewModel.getSelectedImages().map { image ->
                SelectedImage(
                    uri = image.toUri(),
                    name = image.name,
                    size = image.size,
                    width = image.width,
                    height = image.height,
                    mediaId = image.mediaId
                )
            }
            onResult(ImagePickerResult.Success(selectedImages))
        }
    )
}

/**
 * Clears the thumbnail cache to free memory.
 * Call this when the picker is dismissed or when memory pressure is high.
 */
fun clearImagePickerCache() {
    com.samoba.imagepickkit.ui.components.ThumbnailCache.clearCache()
}
