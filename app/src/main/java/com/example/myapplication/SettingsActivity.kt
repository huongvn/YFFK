package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.BuildConfig
import com.example.myapplication.mqtt.MqttController
import java.net.Inet4Address
import java.net.NetworkInterface

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etBroker: EditText
    private lateinit var etClientId: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etTopic: EditText
    private lateinit var btnSave: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnSaveTimer: Button
    private lateinit var btnResetCounter: Button
    private lateinit var etMaxMinutes: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var etApiKey: EditText
    private lateinit var etPlaylists: EditText
    private lateinit var btnSaveYoutube: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("yffk_mqtt", MODE_PRIVATE)
        etBroker = findViewById(R.id.et_broker)
        etClientId = findViewById(R.id.et_client_id)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etTopic = findViewById(R.id.et_topic)
        btnSave = findViewById(R.id.btn_save)
        btnConnect = findViewById(R.id.btn_connect)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        btnSaveTimer = findViewById(R.id.btn_save_timer)
        btnResetCounter = findViewById(R.id.btn_reset_counter)
        etMaxMinutes = findViewById(R.id.et_max_minutes)
        tvStatus = findViewById(R.id.tv_status)
        tvIp = findViewById(R.id.tv_ip)
        etApiKey = findViewById(R.id.et_api_key)
        etPlaylists = findViewById(R.id.et_playlists)
        btnSaveYoutube = findViewById(R.id.btn_save_youtube)
        tvIp.text = "IP local: ${getLocalIpAddress()}"

        SessionTimer.load(prefs)

        etBroker.setText(prefs.getString("mqtt_broker", "tcp://broker.hivemq.com:1883"))
        etClientId.setText(prefs.getString("mqtt_client_id", ""))
        etUsername.setText(prefs.getString("mqtt_username", ""))
        etPassword.setText(prefs.getString("mqtt_password", ""))
        etTopic.setText(prefs.getString("mqtt_topic", "yffk/youtube-tv/command"))
        etMaxMinutes.setText(SessionTimer.maxMinutes.toString())

        etApiKey.setText(prefs.getString("yt_api_key", "") ?: "")
        if (etApiKey.text.isEmpty()) etApiKey.setText(BuildConfig.YOUTUBE_API_KEY)
        etPlaylists.setText(prefs.getString("yt_playlist_ids", "") ?: "")
        if (etPlaylists.text.isEmpty()) etPlaylists.setText(defaultPlaylistIdsJoined())

        MqttController.onState = { status ->
            tvStatus.text = status
            updateButtons()
        }

        tvStatus.text = when {
            MqttController.isConnected() -> "Đã kết nối"
            prefs.getBoolean("mqtt_connected", false) -> "Đang kết nối..."
            else -> "Chưa kết nối"
        }
        updateButtons()

        btnSave.setOnClickListener {
            saveFields()
            tvStatus.text = "Đã lưu cấu hình"
        }

        btnConnect.setOnClickListener {
            saveFields()
            prefs.edit().putBoolean("mqtt_connected", true).apply()
            val broker = etBroker.text.toString().trim()
            val clientId = etClientId.text.toString().trim()
                .ifEmpty { "yffk-tv-${System.currentTimeMillis()}" }
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val topic = etTopic.text.toString().trim()
            if (broker.isEmpty() || topic.isEmpty()) {
                tvStatus.text = "Broker và Topic không được trống"
                return@setOnClickListener
            }
            MqttController.connect(broker, clientId, topic, username, password)
        }

        btnDisconnect.setOnClickListener {
            MqttController.disconnect()
            prefs.edit().putBoolean("mqtt_connected", false).apply()
        }

        btnSaveTimer.setOnClickListener {
            val value = etMaxMinutes.text.toString().toIntOrNull()
            if (value == null || value <= 0) {
                tvStatus.text = "Số phút tối đa phải lớn hơn 0"
                return@setOnClickListener
            }
            SessionTimer.maxMinutes = value
            SessionTimer.save(prefs)
            tvStatus.text = "Đã lưu thông số ($value phút)"
        }

        btnResetCounter.setOnClickListener {
            SessionTimer.reset()
            Toast.makeText(this, "Đã reset bộ đếm", Toast.LENGTH_SHORT).show()
        }

        btnSaveYoutube.setOnClickListener {
            val ids = etPlaylists.text.toString()
                .split('\n', ',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(20)
            prefs.edit()
                .putString("yt_api_key", etApiKey.text.toString().trim())
                .putString("yt_playlist_ids", ids.joinToString("\n"))
                .apply()
            tvStatus.text = "Đã lưu cấu hình YouTube"
            Toast.makeText(this, "Đã lưu, quay lại để tải lại trang chính", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun defaultPlaylistIdsJoined(): String {
        val result = mutableListOf<String>()
        for (i in 1..20) {
            val field = try {
                BuildConfig::class.java.getField("YOUTUBE_PLAYLIST_ID_$i")
            } catch (e: Exception) {
                null
            }
            val value = field?.get(null) as? String
            if (!value.isNullOrEmpty()) result.add(value)
        }
        return result.joinToString("\n")
    }

    private fun saveFields() {
        val broker = etBroker.text.toString().trim()
        val clientId = etClientId.text.toString().trim()
            .ifEmpty { "yffk-tv-${System.currentTimeMillis()}" }
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val topic = etTopic.text.toString().trim()
        prefs.edit()
            .putString("mqtt_broker", broker)
            .putString("mqtt_client_id", clientId)
            .putString("mqtt_username", username)
            .putString("mqtt_password", password)
            .putString("mqtt_topic", topic)
            .apply()
    }

    private fun updateButtons() {
        val connected = MqttController.isConnected()
        btnConnect.isEnabled = !connected
        btnDisconnect.isEnabled = connected
    }

    override fun onDestroy() {
        super.onDestroy()
        MqttController.onState = null
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "Không xác định"
                    }
                }
            }
            "Không xác định"
        } catch (e: Exception) {
            e.printStackTrace()
            "Không xác định"
        }
    }
}
