package com.samoba.imagepickkit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samoba.imagepickkit.ImagePickerConfig
import com.samoba.imagepickkit.domain.Folder
import com.samoba.imagepickkit.domain.FolderImages
import com.samoba.imagepickkit.domain.Image
import com.samoba.imagepickkit.domain.ImagePickerRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the image picker screen.
 * Handles image/folder loading and selection logic.
 */
internal class ImagePickerViewModel(
    private val imagePickerRepository: ImagePickerRepository,
    private val config: ImagePickerConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagePickerState())
    val uiState: StateFlow<ImagePickerState> = _uiState.asStateFlow()

    // Maximum number of IDs to preload for "Select All" functionality
    private val maxPreloadIds = minOf(config.maxSelection, 3000)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load paging flows
            val imagesFlow = imagePickerRepository.imagesInFolderPagingDataFlow(null)
            val foldersFlow = imagePickerRepository.foldersPagingDataFlow()

            // Load initial selection if provided
            val initialSelectedImages = if (config.selectedUris.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    imagePickerRepository.getImagesByUris(config.selectedUris)
                }
            } else {
                emptyList()
            }

            // Preload image IDs for "Select All" functionality
            val allIds = withContext(Dispatchers.IO) {
                imagePickerRepository.getAllImageIds(maxPreloadIds)
            }

            _uiState.update { current ->
                current.copy(
                    images = imagesFlow,
                    folders = foldersFlow,
                    selectedImages = initialSelectedImages.toPersistentList(),
                    selectedImageIds = initialSelectedImages.map { it.mediaId }.toPersistentSet(),
                    allPhotosIds = allIds.toPersistentSet(),
                    maxSelectionReached = initialSelectedImages.size >= config.maxSelection
                )
            }
            
            updateSelectionFlags()
        }
    }

    fun onEvent(event: ImagePickerEvent) {
        when (event) {
            is ImagePickerEvent.FolderClicked -> onFolderClicked(event.folder)
            is ImagePickerEvent.ImageClicked -> onImageClicked(event.image)
            ImagePickerEvent.ClearSelectedImages -> clearSelection()
            is ImagePickerEvent.SelectAll -> onSelectAll(event.folderId)
        }
    }

    private fun onFolderClicked(folder: Folder?) {
        viewModelScope.launch {
            if (folder == null) {
                // Going back to folder list
                _uiState.update { it.copy(folderImages = null, currentFolderImageIds = persistentSetOf()) }
            } else {
                // Entering a folder
                val folderImagesFlow = imagePickerRepository.imagesInFolderPagingDataFlow(folder.id)
                
                // Load folder image IDs for "Select All in folder"
                val folderIds = withContext(Dispatchers.IO) {
                    imagePickerRepository.getImagesInFolder(folder.id, maxPreloadIds)
                        .map { it.mediaId }
                        .toSet()
                }

                _uiState.update { current ->
                    current.copy(
                        folderImages = FolderImages(folderImagesFlow, folder),
                        currentFolderImageIds = folderIds.toPersistentSet()
                    )
                }
                
                updateSelectionFlags()
            }
        }
    }

    private fun onImageClicked(image: Image) {
        val currentState = _uiState.value
        val isSelected = image.mediaId in currentState.selectedImageIds

        if (isSelected) {
            // Deselect
            val newSelection = currentState.selectedImages.filter { it.mediaId != image.mediaId }
            _uiState.update { current ->
                current.copy(
                    selectedImages = newSelection.toPersistentList(),
                    selectedImageIds = newSelection.map { it.mediaId }.toPersistentSet(),
                    maxSelectionReached = false
                )
            }
        } else {
            // Check max selection
            if (currentState.selectedImages.size >= config.maxSelection) {
                _uiState.update { current ->
                    current.copy(
                        message = "Maximum of ${config.maxSelection} images allowed",
                        maxSelectionReached = true
                    )
                }
                return
            }

            // Select
            val newSelection = currentState.selectedImages + image
            _uiState.update { current ->
                current.copy(
                    selectedImages = newSelection.toPersistentList(),
                    selectedImageIds = newSelection.map { it.mediaId }.toPersistentSet(),
                    maxSelectionReached = newSelection.size >= config.maxSelection
                )
            }
        }

        updateSelectionFlags()
    }

    private fun clearSelection() {
        _uiState.update { current ->
            current.copy(
                selectedImages = persistentListOf(),
                selectedImageIds = persistentSetOf(),
                isSelectedAll = false,
                currentFolderSelected = false,
                maxSelectionReached = false
            )
        }
    }

    private fun onSelectAll(folderId: Long?) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            val idsToToggle = if (folderId != null) {
                currentState.currentFolderImageIds
            } else {
                currentState.allPhotosIds
            }

            val currentlySelected = currentState.selectedImageIds
            val allSelected = idsToToggle.all { it in currentlySelected }

            if (allSelected) {
                // Deselect all in scope
                val newSelection = currentState.selectedImages.filter { it.mediaId !in idsToToggle }
                _uiState.update { current ->
                    current.copy(
                        selectedImages = newSelection.toPersistentList(),
                        selectedImageIds = newSelection.map { it.mediaId }.toPersistentSet(),
                        maxSelectionReached = false
                    )
                }
            } else {
                // Select all in scope (up to max)
                val currentIds = currentState.selectedImageIds
                val idsToAdd = idsToToggle.filter { it !in currentIds }
                val availableSlots = config.maxSelection - currentState.selectedImages.size
                val idsToActuallyAdd = idsToAdd.take(availableSlots)

                if (idsToActuallyAdd.isNotEmpty()) {
                    val imagesToAdd = withContext(Dispatchers.IO) {
                        imagePickerRepository.getImagesById(idsToActuallyAdd)
                    }
                    val newSelection = currentState.selectedImages + imagesToAdd
                    _uiState.update { current ->
                        current.copy(
                            selectedImages = newSelection.toPersistentList(),
                            selectedImageIds = newSelection.map { it.mediaId }.toPersistentSet(),
                            maxSelectionReached = newSelection.size >= config.maxSelection
                        )
                    }
                }

                if (idsToActuallyAdd.size < idsToAdd.size) {
                    _uiState.update { current ->
                        current.copy(
                            message = "Maximum of ${config.maxSelection} images allowed"
                        )
                    }
                }
            }

            updateSelectionFlags()
        }
    }

    private fun updateSelectionFlags() {
        val currentState = _uiState.value
        val selectedIds = currentState.selectedImageIds

        val isSelectedAll = currentState.allPhotosIds.isNotEmpty() &&
                currentState.allPhotosIds.all { it in selectedIds }

        val currentFolderSelected = currentState.currentFolderImageIds.isNotEmpty() &&
                currentState.currentFolderImageIds.all { it in selectedIds }

        _uiState.update { current ->
            current.copy(
                isSelectedAll = isSelectedAll,
                currentFolderSelected = currentFolderSelected
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun getSelectedImages(): List<Image> = _uiState.value.selectedImages
}
