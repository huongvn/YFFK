package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.example.myapplication.model.PlaylistItem
import com.example.myapplication.model.YouTubeResponse
import com.example.myapplication.network.YouTubeApiService
import com.example.myapplication.ui.VideoCardPresenter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : FragmentActivity() {

    private val API_KEY = "REMOVED_API_KEY"
    private val PLAYLIST_ID = "PLYzKnm87_04Y"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = BrowseSupportFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commitNow()

        fragment.title = "YouTube Playlist"

        fetchPlaylistVideos(fragment)
    }

    private fun fetchPlaylistVideos(fragment: BrowseSupportFragment) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(YouTubeApiService::class.java)
        service.getPlaylistItems(playlistId = PLAYLIST_ID, apiKey = API_KEY)
            .enqueue(object : Callback<YouTubeResponse> {
                override fun onResponse(
                    call: Call<YouTubeResponse>,
                    response: Response<YouTubeResponse>
                ) {
                    if (response.isSuccessful) {
                        val allItems = response.body()?.items ?: emptyList()
                        val embeddableItems = allItems.filter { it.status.embeddable }
                        setupRows(fragment, embeddableItems)
                    }
                }

                override fun onFailure(call: Call<YouTubeResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun setupRows(fragment: BrowseSupportFragment, items: List<PlaylistItem>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = VideoCardPresenter()
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)

        items.forEach { listRowAdapter.add(it) }

        val header = HeaderItem(0, "Playlist Videos")
        rowsAdapter.add(ListRow(header, listRowAdapter))
        fragment.adapter = rowsAdapter

        fragment.onItemViewClickedListener = OnItemViewClickedListener { itemClickDetector, item, _, _ ->
            if (item is PlaylistItem) {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("VIDEO_ID", item.snippet.resourceId.videoId)
                startActivity(intent)
            }
        }
    }
}