package com.example.myapplication.model

data class YouTubeResponse(
    val items: List<PlaylistItem>? = null
)

data class PlaylistItem(
    val snippet: Snippet? = null,
    val status: Status? = null
)

data class Snippet(
    val title: String? = null,
    val resourceId: ResourceId? = null,
    val thumbnails: Thumbnails? = null
)

data class Status(
    val embeddable: Boolean? = null,
    val privacyStatus: String? = null
)

data class ResourceId(
    val videoId: String? = null
)

data class Thumbnails(
    val medium: ThumbnailUrl? = null
)

data class ThumbnailUrl(
    val url: String? = null
)
