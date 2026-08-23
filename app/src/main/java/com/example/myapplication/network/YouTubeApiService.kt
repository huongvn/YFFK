package com.example.myapplication.network

import com.example.myapplication.model.YouTubeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("youtube/v3/playlistItems")
    fun getPlaylistItems(
        @Query("part") part: String = "snippet,status",
        @Query("playlistId") playlistId: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int = 50
    ): Call<YouTubeResponse>
}