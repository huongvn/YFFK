package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.mqtt.MqttController

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etBroker: EditText
    private lateinit var etClientId: EditText
    private lateinit var etTopic: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("yffk_mqtt", MODE_PRIVATE)
        etBroker = findViewById(R.id.et_broker)
        etClientId = findViewById(R.id.et_client_id)
        etTopic = findViewById(R.id.et_topic)
        btnConnect = findViewById(R.id.btn_connect)
        tvStatus = findViewById(R.id.tv_status)

        etBroker.setText(prefs.getString("mqtt_broker", "tcp://broker.hivemq.com:1883"))
        etClientId.setText(prefs.getString("mqtt_client_id", ""))
        etTopic.setText(prefs.getString("mqtt_topic", "yffk/youtube-tv/command"))

        MqttController.onState = { status ->
            tvStatus.text = status
            updateButton()
        }

        updateButton()

        btnConnect.setOnClickListener {
            if (MqttController.isConnected()) {
                MqttController.disconnect()
                prefs.edit().putBoolean("mqtt_connected", false).apply()
            } else {
                val broker = etBroker.text.toString().trim()
                val clientId = etClientId.text.toString().trim()
                    .ifEmpty { "yffk-tv-${System.currentTimeMillis()}" }
                val topic = etTopic.text.toString().trim()
                if (broker.isEmpty() || topic.isEmpty()) {
                    tvStatus.text = "Broker và Topic không được trống"
                    return@setOnClickListener
                }
                prefs.edit()
                    .putString("mqtt_broker", broker)
                    .putString("mqtt_client_id", clientId)
                    .putString("mqtt_topic", topic)
                    .putBoolean("mqtt_connected", true)
                    .apply()
                MqttController.connect(broker, clientId, topic)
            }
            updateButton()
        }
    }

    private fun updateButton() {
        btnConnect.text = if (MqttController.isConnected()) "Ngắt kết nối" else "Kết nối"
    }

    override fun onDestroy() {
        super.onDestroy()
        MqttController.onState = null
    }
}
