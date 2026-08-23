package com.example.myapplication.model

data class PlaylistTitleResponse(
    val items: List<PlaylistTitleItem>? = null
)

data class PlaylistTitleItem(
    val snippet: PlaylistTitleSnippet? = null
)

data class PlaylistTitleSnippet(
    val title: String? = null
)
