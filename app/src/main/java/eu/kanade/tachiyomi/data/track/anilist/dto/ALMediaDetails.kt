package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALMediaDetails(
    val id: Int,
    val type: String,
    val title: String,
    val titleEnglish: String?,
    val description: String?,
    val coverUrl: String?,
    val bannerUrl: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val format: String?,
    val status: String?,
    val genres: List<String>,
    val isAdult: Boolean,
    val averageScore: Int?,
    val popularity: Int?,
    val source: String?,
    val siteUrl: String?,
    val studios: List<String>,
    val listStatus: String?,
)
