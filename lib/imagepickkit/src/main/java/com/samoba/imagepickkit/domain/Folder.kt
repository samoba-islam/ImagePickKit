package com.samoba.imagepickkit.domain

import java.time.LocalDateTime

/**
 * Represents a folder (bucket) containing images.
 */
data class Folder(
    val id: Long,
    val path: String,
    val lastAddedDate: LocalDateTime,
    val name: String,
    val imagesCount: Int,
    val imagesSize: Long,
)
