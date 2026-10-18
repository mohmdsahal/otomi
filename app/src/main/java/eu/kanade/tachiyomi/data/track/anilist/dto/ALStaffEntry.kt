package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALStaffEntry(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val role: String?,
)
