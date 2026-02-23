package com.samoba.imagepickkit.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.samoba.imagepickkit.domain.Image
import com.samoba.imagepickkit.ui.components.ImageCard
import kotlinx.collections.immutable.ImmutableSet

/**
 * Tab displaying a grid of photos.
 */
@Composable
internal fun PhotosTab(
    modifier: Modifier = Modifier,
    images: LazyPagingItems<Image>?,
    selectedImageIds: ImmutableSet<Long>,
    gridColumns: Int = 3,
    onImageClicked: (Image) -> Unit,
    preContent: LazyGridScope.() -> Unit = {}
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag("images_tab"),
    ) {
        preContent()

        when (images?.loadState?.refresh) {
            is LoadState.Error -> Unit

            LoadState.Loading, null -> {
                items(20) {
                    Placeholder()
                }
            }

            is LoadState.NotLoading -> {
                items(
                    count = images.itemCount,
                    key = images.itemKey { it.mediaId },
                    contentType = { 1 }
                ) { index ->
                    val item = images[index]

                    if (item != null) {
                        ImageCard(
                            image = item,
                            selected = item.mediaId in selectedImageIds,
                            onClick = onImageClicked
                        )
                    } else {
                        Placeholder()
                    }
                }
            }
        }
    }
}

@Composable
internal fun Placeholder() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1F)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.extraSmall
            )
    )
}
