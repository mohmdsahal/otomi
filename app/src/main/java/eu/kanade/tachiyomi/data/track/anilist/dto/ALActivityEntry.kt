package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALActivityEntry(
    val id: Int,
    val type: String,
    val userId: Int?,
    val userName: String?,
    val userAvatarUrl: String?,
    val status: String?,
    val progress: String?,
    val text: String?,
    val mediaId: Int?,
    val mediaTitle: String?,
    val mediaCoverUrl: String?,
    val createdAt: Int,
    val siteUrl: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val replyCount: Int,
)
