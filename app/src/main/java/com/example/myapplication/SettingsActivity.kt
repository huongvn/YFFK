package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView

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
        tvStatus = findViewById(R.id.tv_status)
        tvIp = findViewById(R.id.tv_ip)
        tvIp.text = "IP local: ${getLocalIpAddress()}"

        etBroker.setText(prefs.getString("mqtt_broker", "tcp://broker.hivemq.com:1883"))
        etClientId.setText(prefs.getString("mqtt_client_id", ""))
        etUsername.setText(prefs.getString("mqtt_username", ""))
        etPassword.setText(prefs.getString("mqtt_password", ""))
        etTopic.setText(prefs.getString("mqtt_topic", "yffk/youtube-tv/command"))

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
