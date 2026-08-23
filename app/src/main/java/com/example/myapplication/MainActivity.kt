package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.ImageView
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
import com.example.myapplication.mqtt.MqttController
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
    private val playlistIds: List<String> by lazy {
        listOf(
            BuildConfig.YOUTUBE_PLAYLIST_ID,
            BuildConfig.YOUTUBE_PLAYLIST_ID_2,
            BuildConfig.YOUTUBE_PLAYLIST_ID_3
        ).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private lateinit var tvClock: TextView
    private lateinit var tvPlaylistName: TextView
    private lateinit var tvUptime: TextView
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private val playlistVideoMap = mutableMapOf<String, Pair<List<String>, Int>>()
    private var rowId = 0

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            tvClock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            clockHandler.postDelayed(this, 1000)
        }
    }
    private val uptimeHandler = Handler(Looper.getMainLooper())
    private val uptimeRunnable = object : Runnable {
        override fun run() {
            tvUptime.text = "${SessionTimer.elapsedMinutes()} phút"
            uptimeHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvClock = findViewById(R.id.tv_clock)
        tvPlaylistName = findViewById(R.id.tv_playlist_name)
        tvPlaylistName.text = "YFFK"
        clockHandler.post(clockRunnable)

        tvUptime = findViewById(R.id.tv_uptime)
        val prefs = getSharedPreferences("yffk_mqtt", MODE_PRIVATE)
        SessionTimer.load(prefs)
        SessionTimer.startTime = SystemClock.elapsedRealtime()
        uptimeHandler.post(uptimeRunnable)

        findViewById<ImageView>(R.id.iv_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        autoConnectMqtt()

        val fragment = BrowseSupportFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commitNow()

        fragment.headersState = BrowseSupportFragment.HEADERS_ENABLED

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        fragment.adapter = rowsAdapter
        fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is PlaylistItem) {
                val videoId = item.snippet.resourceId.videoId ?: return@OnItemViewClickedListener
                val entry = playlistVideoMap[videoId] ?: return@OnItemViewClickedListener
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putStringArrayListExtra("VIDEO_IDS", ArrayList(entry.first))
                intent.putExtra("INDEX", entry.second)
                startActivity(intent)
            }
        }

        playlistIds.forEach { loadPlaylist(fragment, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        uptimeHandler.removeCallbacks(uptimeRunnable)
    }

    private fun autoConnectMqtt() {
        val prefs = getSharedPreferences("yffk_mqtt", MODE_PRIVATE)
        if (!prefs.getBoolean("mqtt_connected", false)) return
        val broker = prefs.getString("mqtt_broker", "") ?: ""
        val clientId = prefs.getString("mqtt_client_id", "") ?: ""
        val username = prefs.getString("mqtt_username", "") ?: ""
        val password = prefs.getString("mqtt_password", "") ?: ""
        val topic = prefs.getString("mqtt_topic", "") ?: ""
        if (broker.isNotEmpty() && topic.isNotEmpty()) {
            MqttController.connect(broker, clientId, topic, username, password)
        }
    }

    private fun loadPlaylist(fragment: BrowseSupportFragment, playlistId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(YouTubeApiService::class.java)

        fun buildRow(title: String) {
            service.getPlaylistItems(playlistId = playlistId, apiKey = API_KEY)
                .enqueue(object : Callback<YouTubeResponse> {
                    override fun onResponse(call: Call<YouTubeResponse>, response: Response<YouTubeResponse>) {
                        if (!response.isSuccessful) return
                        val allItems = response.body()?.items ?: emptyList()
                        val embeddableItems = allItems.filter { it.status.embeddable != false }
                        val displayItems = if (embeddableItems.isEmpty()) allItems else embeddableItems
                        if (displayItems.isEmpty()) return

                        val videoIds = displayItems.mapNotNull { it.snippet.resourceId.videoId }
                        displayItems.forEachIndexed { idx, it ->
                            val vid = it.snippet.resourceId.videoId
                            if (vid != null) playlistVideoMap[vid] = Pair(videoIds, idx)
                        }

                        val header = HeaderItem(rowId++.toLong(), title)
                        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                        displayItems.forEach { listRowAdapter.add(it) }
                        rowsAdapter.add(ListRow(header, listRowAdapter))
                    }

                    override fun onFailure(call: Call<YouTubeResponse>, t: Throwable) {
                        t.printStackTrace()
                    }
                })
        }

        service.getPlaylist(playlistId = playlistId, apiKey = API_KEY)
            .enqueue(object : Callback<PlaylistTitleResponse> {
                override fun onResponse(
                    call: Call<PlaylistTitleResponse>,
                    response: Response<PlaylistTitleResponse>
                ) {
                    val title = response.body()?.items?.firstOrNull()?.snippet?.title ?: playlistId
                    buildRow(title)
                }

                override fun onFailure(call: Call<PlaylistTitleResponse>, t: Throwable) {
                    t.printStackTrace()
                    buildRow(playlistId)
                }
            })
    }
}
