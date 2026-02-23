package com.samoba.imagepickkit.ui.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.samoba.imagepickkit.domain.Folder
import com.samoba.imagepickkit.domain.FolderImages
import com.samoba.imagepickkit.domain.Image
import com.samoba.imagepickkit.ui.components.FolderListTile
import kotlinx.collections.immutable.ImmutableSet

/**
 * Tab displaying folders with the ability to drill into a folder's images.
 */
@Composable
internal fun FoldersTab(
    modifier: Modifier = Modifier,
    folders: LazyPagingItems<Folder>,
    folderImagesPagingItems: LazyPagingItems<Image>?,
    selectedImageIds: ImmutableSet<Long>,
    folderImages: FolderImages?,
    gridColumns: Int = 3,
    onImageClicked: (Image) -> Unit,
    onFolderClicked: (Folder?) -> Unit
) {
    AnimatedContent(
        folderImages,
        label = "ContentAnimator",
        transitionSpec = {
            if (initialState?.folder == targetState?.folder) {
                EnterTransition.None.togetherWith(ExitTransition.None)
            } else if (targetState != null) {
                (fadeIn() + slideInHorizontally { it / 2 }).togetherWith(ExitTransition.None)
            } else {
                fadeIn().togetherWith(slideOutHorizontally { it })
            }
        }
    ) { content ->
        if (content == null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = modifier
                    .fillMaxSize()
                    .testTag("folders_tab")
            ) {
                items(folders.itemCount, key = folders.itemKey { it.id }) {
                    val item = folders[it]

                    if (item != null) {
                        FolderListTile(
                            folder = item,
                            onClick = { onFolderClicked(item) }
                        )
                    }
                }
            }
        } else {
            BackHandler { onFolderClicked(null) }

            Surface(
                modifier = Modifier.fillMaxSize(),
            ) {
                PhotosTab(
                    images = folderImagesPagingItems,
                    selectedImageIds = selectedImageIds,
                    gridColumns = gridColumns,
                    onImageClicked = onImageClicked,
                    modifier = modifier
                ) {
                    item(
                        key = "header",
                        contentType = { 2 },
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderClicked(null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to folders",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    content.folder.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
