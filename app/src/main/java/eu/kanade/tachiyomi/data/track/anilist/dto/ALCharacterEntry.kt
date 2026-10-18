package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALCharacterEntry(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val role: String?,
    val voiceActorName: String?,
    val voiceActorImageUrl: String?,
)
