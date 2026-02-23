package com.samoba.imagepickkit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.samoba.imagepickkit.ImagePickerBarState
import com.samoba.imagepickkit.ImagePickerConfig
import com.samoba.imagepickkit.SelectedImage
import com.samoba.imagepickkit.domain.toUri
import com.samoba.imagepickkit.ui.tabs.FoldersTab
import com.samoba.imagepickkit.ui.tabs.PhotosTab
import kotlinx.coroutines.launch

/**
 * Internal composable for the image picker screen content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImagePickerScreenContent(
    viewModel: ImagePickerViewModel,
    config: ImagePickerConfig,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Show messages as snackbars
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })

    // Build the bar state for custom composables
    val barState = ImagePickerBarState(
        selectedCount = state.selectedImages.size,
        selectedImages = state.selectedImages.map { image ->
            SelectedImage(
                uri = image.toUri(),
                name = image.name,
                size = image.size,
                width = image.width,
                height = image.height,
                mediaId = image.mediaId
            )
        },
        isSelectAllChecked = if (state.folderImages != null) state.currentFolderSelected else state.isSelectedAll,
        maxSelectionReached = state.maxSelectionReached
    )

    // Lambdas for TopBar actions
    val onSelectAllClick: () -> Unit = {
        val folderId = state.folderImages?.folder?.id
        viewModel.onEvent(ImagePickerEvent.SelectAll(folderId))
    }
    val onClearSelectionClick: () -> Unit = {
        viewModel.onEvent(ImagePickerEvent.ClearSelectedImages)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (config.topBar != null) {
                config.topBar.invoke(
                    barState,
                    onBackClick,
                    onSelectAllClick,
                    onClearSelectionClick
                )
            } else {
                DefaultImagePickerTopBar(
                    title = config.title,
                    scrollBehavior = scrollBehavior,
                    selectedCount = state.selectedImages.size,
                    isSelectedAll = barState.isSelectAllChecked,
                    showSelectAll = config.showSelectAll && (state.selectedImages.isNotEmpty() || state.folderImages != null),
                    onBackClick = onBackClick,
                    onSelectAll = onSelectAllClick,
                    onClearSelection = onClearSelectionClick
                )
            }
        },
        bottomBar = {
            if (state.selectedImages.isNotEmpty()) {
                if (config.bottomBar != null) {
                    config.bottomBar.invoke(barState, onContinueClick)
                } else {
                    DefaultImagePickerBottomBar(
                        text = "${config.continueButtonText} (${state.selectedImages.size})",
                        onClick = onContinueClick
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ImagePickerTabs(
                selectedTabIndex = pagerState.currentPage,
                photosTabText = config.photosTabText,
                foldersTabText = config.foldersTabText,
                onTabClicked = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        val images = state.images?.collectAsLazyPagingItems()
                        PhotosTab(
                            images = images,
                            selectedImageIds = state.selectedImageIds,
                            gridColumns = config.gridColumns,
                            onImageClicked = { image ->
                                viewModel.onEvent(ImagePickerEvent.ImageClicked(image))
                            }
                        )
                    }

                    1 -> {
                        val folders = state.folders?.collectAsLazyPagingItems()
                        val folderImagesPaging = state.folderImages?.images?.collectAsLazyPagingItems()
                        
                        if (folders != null) {
                            FoldersTab(
                                folders = folders,
                                folderImagesPagingItems = folderImagesPaging,
                                selectedImageIds = state.selectedImageIds,
                                folderImages = state.folderImages,
                                gridColumns = config.gridColumns,
                                onImageClicked = { image ->
                                    viewModel.onEvent(ImagePickerEvent.ImageClicked(image))
                                },
                                onFolderClicked = { folder ->
                                    viewModel.onEvent(ImagePickerEvent.FolderClicked(folder))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Default TopBar composable for the image picker.
 * Can be used as a reference for custom implementations.
 *
 * @param title Title displayed in the TopBar
 * @param scrollBehavior Scroll behavior for collapsing TopBar
 * @param selectedCount Number of currently selected images
 * @param isSelectedAll Whether all images are selected
 * @param showSelectAll Whether to show the "Select All" checkbox
 * @param onBackClick Callback when back button is clicked
 * @param onSelectAll Callback when select all is toggled
 * @param onClearSelection Callback when selection is cleared
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultImagePickerTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    selectedCount: Int,
    isSelectedAll: Boolean,
    showSelectAll: Boolean,
    onBackClick: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                if (selectedCount > 0) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (showSelectAll) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectAll)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isSelectedAll,
                        onCheckedChange = { onSelectAll() }
                    )
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (selectedCount > 0) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun ImagePickerTabs(
    selectedTabIndex: Int,
    photosTabText: String,
    foldersTabText: String,
    onTabClicked: (Int) -> Unit
) {
    val tabs = listOf(photosTabText, foldersTabText)

    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTabIndex == index
            val textColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "tab_color"
            )

            Tab(
                selected = selected,
                onClick = { onTabClicked(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            )
        }
    }
}

/**
 * Default BottomBar composable for the image picker.
 * Can be used as a reference for custom implementations.
 *
 * @param text Text to display on the button
 * @param onClick Callback when the button is clicked
 */
@Composable
fun DefaultImagePickerBottomBar(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    }
}
