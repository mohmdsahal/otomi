package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALStudioEntry(
    val id: Int,
    val name: String,
    val isMain: Boolean,
    val siteUrl: String?,
)
