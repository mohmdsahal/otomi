package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALMediaPreview(
    val id: Int,
    val type: String,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val status: String?,
    val averageScore: Int?,
    val popularity: Int?,
    val startYear: Int?,
    val isAdult: Boolean,
    val listStatus: String?,
)
