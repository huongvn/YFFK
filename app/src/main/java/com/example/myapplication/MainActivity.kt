package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.view.KeyEvent
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
import androidx.leanback.widget.Row
import com.example.myapplication.model.PlaylistItem
import com.example.myapplication.model.PlaylistTitleResponse
import com.example.myapplication.model.YouTubeResponse
import com.example.myapplication.mqtt.MqttController
import com.example.myapplication.network.YouTubeApiService
import com.example.myapplication.ui.PlaylistHeaderPresenter
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

    private var apiKey: String = BuildConfig.YOUTUBE_API_KEY
    private var playlistIds: List<String> = emptyList()

    private lateinit var tvClock: TextView
    private lateinit var tvPlaylistName: TextView
    private lateinit var tvUptime: TextView
    private lateinit var ivSettings: ImageView
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var fragment: BrowseSupportFragment
    private val playlistVideoMap = mutableMapOf<String, Pair<List<String>, Int>>()
    private var rowId = 0
    private var lastConfigSignature: String = ""
    private var currentRowPosition = 0

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val youTubeService by lazy { retrofit.create(YouTubeApiService::class.java) }
    private val pendingCalls = mutableListOf<Call<*>>()

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

        loadConfig(prefs)

        ivSettings = findViewById(R.id.iv_settings)
        ivSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        autoConnectMqtt()

        fragment = BrowseSupportFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commitNow()

        fragment.headersState = BrowseSupportFragment.HEADERS_ENABLED

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter().apply {
            headerPresenter = PlaylistHeaderPresenter()
        })
        fragment.adapter = rowsAdapter
        fragment.setOnItemViewSelectedListener { _, _, _, row ->
            currentRowPosition = (row as? Row)?.let { rowsAdapter.indexOf(it) } ?: 0
        }
        fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is PlaylistItem) {
                val videoId = item.snippet?.resourceId?.videoId ?: return@OnItemViewClickedListener
                val entry = playlistVideoMap[videoId] ?: return@OnItemViewClickedListener
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putStringArrayListExtra("VIDEO_IDS", ArrayList(entry.first))
                intent.putExtra("INDEX", entry.second)
                startActivity(intent)
            }
        }

        loadPlaylists()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("yffk_mqtt", MODE_PRIVATE)
        val sig = configSignature(prefs)
        if (sig != lastConfigSignature) {
            loadConfig(prefs)
            loadPlaylists()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (!ivSettings.hasFocus() && currentRowPosition == 0) {
                        ivSettings.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (ivSettings.hasFocus()) {
                        fragment.view?.requestFocus()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun configSignature(prefs: SharedPreferences): String {
        val key = prefs.getString("yt_api_key", "")?.takeIf { it.isNotEmpty() } ?: BuildConfig.YOUTUBE_API_KEY
        val ids = computePlaylistIds(prefs)
        return "$key|${ids.joinToString(",")}"
    }

    private fun loadConfig(prefs: SharedPreferences) {
        apiKey = prefs.getString("yt_api_key", "")?.takeIf { it.isNotEmpty() } ?: BuildConfig.YOUTUBE_API_KEY
        playlistIds = computePlaylistIds(prefs)
        lastConfigSignature = configSignature(prefs)
    }

    private fun computePlaylistIds(prefs: SharedPreferences): List<String> {
        val raw = prefs.getString("yt_playlist_ids", "") ?: ""
        val fromPrefs = raw.split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (fromPrefs.isNotEmpty()) {
            fromPrefs.take(20)
        } else {
            val defaults = mutableListOf<String>()
            for (i in 1..20) {
                val field = try {
                    BuildConfig::class.java.getField("YOUTUBE_PLAYLIST_ID_$i")
                } catch (e: Exception) {
                    null
                }
                val value = field?.get(null) as? String
                if (!value.isNullOrEmpty()) defaults.add(value)
            }
            defaults.take(20)
        }
    }

    private fun loadPlaylists() {
        pendingCalls.forEach { it.cancel() }
        pendingCalls.clear()
        playlistVideoMap.clear()
        rowId = 0
        rowsAdapter.clear()
        playlistIds.forEach { loadPlaylist(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingCalls.forEach { it.cancel() }
        pendingCalls.clear()
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

    private fun loadPlaylist(playlistId: String) {
        fun buildRow(title: String) {
            val itemsCall = youTubeService.getPlaylistItems(playlistId = playlistId, apiKey = apiKey)
            pendingCalls.add(itemsCall)
            itemsCall.enqueue(object : Callback<YouTubeResponse> {
                override fun onResponse(call: Call<YouTubeResponse>, response: Response<YouTubeResponse>) {
                    pendingCalls.remove(call)
                        if (!response.isSuccessful) return
                        val allItems = response.body()?.items ?: emptyList()
                        val displayItems = allItems.filter { item ->
                            val s = item.status
                            s == null || (s.embeddable != false && s.privacyStatus != "private")
                        }
                        if (displayItems.isEmpty()) return

                    val videoIds = displayItems.mapNotNull { it.snippet?.resourceId?.videoId }
                    displayItems.forEachIndexed { idx, it ->
                        it.snippet?.resourceId?.videoId?.let { vid ->
                            playlistVideoMap[vid] = Pair(videoIds, idx)
                        }
                    }

                    val header = HeaderItem(rowId++.toLong(), title)
                    val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                    displayItems.forEach { listRowAdapter.add(it) }
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }

                override fun onFailure(call: Call<YouTubeResponse>, t: Throwable) {
                    pendingCalls.remove(call)
                    t.printStackTrace()
                }
            })
        }

        val titleCall = youTubeService.getPlaylist(playlistId = playlistId, apiKey = apiKey)
        pendingCalls.add(titleCall)
        titleCall.enqueue(object : Callback<PlaylistTitleResponse> {
            override fun onResponse(
                call: Call<PlaylistTitleResponse>,
                response: Response<PlaylistTitleResponse>
            ) {
                pendingCalls.remove(call)
                val title = response.body()?.items?.firstOrNull()?.snippet?.title ?: playlistId
                buildRow(title)
            }

            override fun onFailure(call: Call<PlaylistTitleResponse>, t: Throwable) {
                pendingCalls.remove(call)
                t.printStackTrace()
                buildRow(playlistId)
            }
        })
    }
}
