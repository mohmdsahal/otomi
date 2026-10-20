package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALFavouriteEntry(
    val id: Int,
    val title: String,
    val imageUrl: String?,
)

@Serializable
data class ALFavourites(
    val anime: List<ALFavouriteEntry>,
    val manga: List<ALFavouriteEntry>,
    val characters: List<ALFavouriteEntry>,
    val staff: List<ALFavouriteEntry>,
    val studios: List<ALFavouriteEntry>,
)
