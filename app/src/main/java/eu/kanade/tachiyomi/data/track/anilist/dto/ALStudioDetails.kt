package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALStudioDetails(
    val id: Int,
    val name: String,
    val favourites: Int?,
    val siteUrl: String?,
    val media: List<ALStudioMediaEntry>,
)

@Serializable
data class ALStudioMediaEntry(
    val id: Int,
    val type: String,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val startYear: Int?,
)
