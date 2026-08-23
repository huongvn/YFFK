package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.example.myapplication.model.PlaylistItem
import com.example.myapplication.model.PlaylistTitleResponse
import com.example.myapplication.model.YouTubeResponse
import com.example.myapplication.network.YouTubeApiService
import com.example.myapplication.ui.VideoCardPresenter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {

    private val API_KEY = BuildConfig.YOUTUBE_API_KEY
    private val PLAYLIST_ID = BuildConfig.YOUTUBE_PLAYLIST_ID

    private lateinit var tvClock: TextView
    private lateinit var tvPlaylistName: TextView
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            tvClock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            clockHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvClock = findViewById(R.id.tv_clock)
        tvPlaylistName = findViewById(R.id.tv_playlist_name)
        tvPlaylistName.text = "YouTube Playlist"
        clockHandler.post(clockRunnable)

        val fragment = BrowseSupportFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commitNow()

        fragment.headersState = BrowseSupportFragment.HEADERS_ENABLED

        fetchPlaylistTitle()
        fetchPlaylistVideos(fragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun fetchPlaylistTitle() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(YouTubeApiService::class.java)
            .getPlaylist(playlistId = PLAYLIST_ID, apiKey = API_KEY)
            .enqueue(object : Callback<PlaylistTitleResponse> {
                override fun onResponse(
                    call: Call<PlaylistTitleResponse>,
                    response: Response<PlaylistTitleResponse>
                ) {
                    val title = response.body()?.items?.firstOrNull()?.snippet?.title
                    if (!title.isNullOrEmpty()) {
                        tvPlaylistName.text = title
                    }
                }

                override fun onFailure(call: Call<PlaylistTitleResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
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
                        val embeddableItems = allItems.filter { it.status.embeddable != false }
                        val displayItems = if (embeddableItems.isEmpty()) allItems else embeddableItems
                        setupRows(fragment, displayItems)
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

        fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is PlaylistItem) {
                val videoIds = items.map { it.snippet.resourceId.videoId }
                val index = items.indexOf(item)
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putStringArrayListExtra("VIDEO_IDS", ArrayList(videoIds))
                intent.putExtra("INDEX", index)
                startActivity(intent)
            }
        }
    }
}
