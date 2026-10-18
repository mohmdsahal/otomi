package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.network.okHttpClient
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUser
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.dataOrElse
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import mihon.graphql.anilist.AniListAddMangaMutation
import mihon.graphql.anilist.AniListDeleteMangaMutation
import mihon.graphql.anilist.AniListGetCurrentUserQuery
import mihon.graphql.anilist.AniListGetLibMangaQuery
import mihon.graphql.anilist.AniListGetMangaDetailsQuery
import mihon.graphql.anilist.AniListSearchMangaQuery
import mihon.graphql.anilist.AniListUpdateMangaMutation
import mihon.graphql.anilist.type.FuzzyDateInput
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import tachiyomi.domain.track.model.Track as DomainTrack
import mihon.graphql.anilist.AniListGetCollectionQuery
import mihon.graphql.anilist.AniListDiscoverMediaQuery
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMediaPreview
import mihon.graphql.anilist.AniListGetMediaDetailsQuery
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMediaDetails

class AnilistApi(
    val trackerId: Long,
    val client: OkHttpClient,
    interceptor: AnilistInterceptor,
) {

    private val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .rateLimit(permits = 25, period = 1.minutes)
        .build()

    private val graphQlClient by lazy {
        ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .okHttpClient(authClient)
            .dispatcher(Dispatchers.IO)
            // required to log the error body in dataOrElse, which also properly closes it
            .httpExposeErrorBody(true)
            .build()
    }

    suspend fun addLibManga(track: Track): Track {
        return graphQlClient
            .mutation(
                AniListAddMangaMutation(
                    manga_id = track.remote_id.toInt(),
                    progress = track.last_chapter_read.toInt(),
                    status = track.toApiStatus(),
                    private = track.private,
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to add manga",
                default = { null },
            ) {
                it.SaveMediaListEntry?.id?.let { libraryId ->
                    track.library_id = libraryId.toLong()
                    track
                }
            }
            ?: throw Exception("Failed to add manga")
    }

    suspend fun updateLibManga(track: Track): Track {
        val libraryId = track.library_id
        requireNotNull(libraryId) { "AniList cannot update track with null library_id" }

        return graphQlClient
            .mutation(
                AniListUpdateMangaMutation(
                    library_id = libraryId.toInt(),
                    progress = track.last_chapter_read.toInt(),
                    status = track.toApiStatus(),
                    private = track.private,
                    score = track.score.toInt(),
                    startedAt = createFuzzyDate(track.started_reading_date),
                    completedAt = createFuzzyDate(track.finished_reading_date),
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to update manga",
                default = { null },
            ) {
                it.SaveMediaListEntry?.id?.let { remoteLibraryId ->
                    track.library_id = remoteLibraryId.toLong()
                    track
                }
            }
            ?: throw Exception("Failed to update manga")
    }

    suspend fun deleteLibManga(track: DomainTrack) {
        val libraryId = track.libraryId
        requireNotNull(libraryId) { "AniList cannot delete track with null library_id" }

        graphQlClient
            .mutation(
                AniListDeleteMangaMutation(library_id = libraryId.toInt()),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to delete manga",
                default = { null },
            ) {
                it.DeleteMediaListEntry?.deleted?.let { deleted ->
                    if (deleted) {
                        logcat { "AniList: Deleted manga ${track.libraryId} successfully" }
                    }
                }
            }
            ?: throw Exception("Failed to delete manga")
    }

    suspend fun search(search: String): List<TrackSearch> {
        return graphQlClient
            .query(
                AniListSearchMangaQuery(search = search),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Search failed",
                default = { emptyList() },
            ) {
                it.Page?.media
                    ?.mapNotNull { alManga -> alManga?.toTrackSearch(trackerId) }
                    ?: emptyList()
            }
    }

    suspend fun findLibManga(track: Track, userId: Int): Track? {
        return graphQlClient
            .query(
                AniListGetLibMangaQuery(
                    user_id = userId,
                    manga_id = track.remote_id.toInt(),
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to find manga in library",
                default = { null },
            ) {
                it.Page?.mediaList
                    ?.firstOrNull()
                    ?.toTrack(trackerId)
            }
    }

    suspend fun getCurrentUser(): ALUser {
        return graphQlClient
            .query(AniListGetCurrentUserQuery())
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to get current user",
                default = { null },
            ) {
                it.Viewer?.toALUser()
            }
            ?: throw Exception("Failed to get AniList user data")
    }
    suspend fun getCollection(userId: Int, type: String): List<TrackSearch> {
        return graphQlClient
            .query(
                AniListGetCollectionQuery(
                    user_id = userId,
                    type = mihon.graphql.anilist.type.MediaType.valueOf(type),
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to get collection",
                default = { emptyList() },
            ) {
                it.MediaListCollection?.lists
                    ?.filterNotNull()
                    ?.flatMap { list -> list.entries.orEmpty() }
                    ?.mapNotNull { entry -> entry?.media?.mangaFragment?.toTrackSearch(trackerId) }
                    ?: emptyList()
            }
    }

    suspend fun discoverMedia(
        page: Int,
        type: String,
        search: String? = null,
        isAdult: Boolean = false,
    ): List<ALMediaPreview> {
        return graphQlClient
            .query(
                AniListDiscoverMediaQuery(
                    page = Optional.present(page),
                    type = Optional.present(mihon.graphql.anilist.type.MediaType.valueOf(type)),
                    search = Optional.presentIfNotNull(search),
                    isAdult = Optional.present(isAdult),
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to discover media",
                default = { emptyList() },
            ) {
                it.Page?.media
                    ?.filterNotNull()
                    ?.map { media ->
                        ALMediaPreview(
                            id = media.id,
                            type = media.type.toString(),
                            title = media.title?.userPreferred.orEmpty(),
                            coverUrl = media.coverImage?.large,
                            format = media.format?.toString(),
                            status = media.status?.toString(),
                            averageScore = media.averageScore,
                            popularity = media.popularity,
                            startYear = media.startDate?.year,
                            isAdult = media.isAdult ?: false,
                            listStatus = media.mediaListEntry?.status?.toString(),
                        )
                    }
                    ?: emptyList()
            }
    }

    suspend fun getMediaInfo(id: Int): ALMediaDetails? {
        return graphQlClient
            .query(
                AniListGetMediaDetailsQuery(id = id),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to get media info",
                default = { null },
            ) { data ->
                data.Media?.let { media ->
                    ALMediaDetails(
                        id = media.id,
                        type = media.type.toString(),
                        title = media.title?.userPreferred.orEmpty(),
                        titleEnglish = media.title?.english,
                        description = media.description,
                        coverUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
                        bannerUrl = media.bannerImage,
                        episodes = media.episodes,
                        chapters = media.chapters,
                        volumes = media.volumes,
                        format = media.format?.toString(),
                        status = media.status?.toString(),
                        genres = media.genres?.filterNotNull().orEmpty(),
                        isAdult = media.isAdult ?: false,
                        averageScore = media.averageScore,
                        popularity = media.popularity,
                        source = media.source?.toString(),
                        siteUrl = media.siteUrl,
                        studios = media.studios?.edges
                            ?.filterNotNull()
                            ?.mapNotNull { it.node?.name }
                            .orEmpty(),
                        listStatus = media.mediaListEntry?.status?.toString(),
                    )
                }
            }
    }

    suspend fun getMangaDetails(id: Int): TrackSearch? {
        return graphQlClient
            .query(
                AniListGetMangaDetailsQuery(manga_id = id),
            )
            .execute()
            .dataOrElse(
                errorLog = "AniList: Failed to get manga details",
                default = { null },
            ) {
                it.Page?.media
                    ?.firstOrNull()
                    ?.toTrackSearch(trackerId)
            }
    }

    private fun createFuzzyDate(dateValue: Long): FuzzyDateInput {
        // all absent/null
        if (dateValue == 0L) return FuzzyDateInput()

        val dateTime = Instant.fromEpochMilliseconds(dateValue).toLocalDateTime(TimeZone.currentSystemDefault())
        return FuzzyDateInput(
            year = Optional.present(dateTime.year),
            month = Optional.present(dateTime.month.number),
            day = Optional.present(dateTime.day),
        )
    }

    companion object {
        private const val CLIENT_ID = "16329"

        fun authUrl(): Uri = "https://anilist.co/api/v2/oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}
