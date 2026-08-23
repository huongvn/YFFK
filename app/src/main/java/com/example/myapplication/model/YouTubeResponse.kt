package com.example.myapplication.model

data class YouTubeResponse(
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val snippet: Snippet,
    val status: Status
)

data class Snippet(
    val title: String,
    val resourceId: ResourceId,
    val thumbnails: Thumbnails
)

data class Status(
    val embeddable: Boolean? = null
)

data class ResourceId(
    val videoId: String
)

data class Thumbnails(
    val medium: ThumbnailUrl
)

data class ThumbnailUrl(
    val url: String
)