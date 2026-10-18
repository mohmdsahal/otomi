package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALCharacterDetails(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val imageUrl: String?,
    val description: String?,
    val birthYear: Int?,
    val birthMonth: Int?,
    val birthDay: Int?,
    val favourites: Int?,
    val siteUrl: String?,
)
