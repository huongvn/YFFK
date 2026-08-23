package com.example.myapplication.mqtt

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

data class MqttCommand(val action: String? = null)

object PlaybackCommandBus {
    private val mainHandler = Handler(Looper.getMainLooper())
    var listener: ((String) -> Unit)? = null

    fun emit(action: String) {
        mainHandler.post { listener?.invoke(action) }
    }
}

private data class ConnectParams(
    val broker: String,
    val clientId: String,
    val topic: String,
    val username: String,
    val password: String
)

object MqttController {

    private const val MAX_ATTEMPTS = 6
    private const val RETRY_DELAY_MS = 5000L

    private var client: MqttClient? = null
    private var shouldBeConnected = false
    private var connectParams: ConnectParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    var onState: ((String) -> Unit)? = null

    fun connect(broker: String, clientId: String, topic: String, username: String = "", password: String = "") {
        shouldBeConnected = true
        connectParams = ConnectParams(broker, clientId, topic, username, password)
        CoroutineScope(Dispatchers.IO).launch {
            attemptConnect(1)
        }
    }

    private suspend fun attemptConnect(attempt: Int) {
        val p = connectParams ?: return
        try {
            disconnectClientOnly()
            val c = MqttClient(p.broker, p.clientId, MemoryPersistence())
            client = c
            c.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    try {
                        c.subscribe(p.topic, 1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    postState(if (reconnect) "Đã kết nối lại MQTT" else "Đã kết nối: ${p.broker}")
                }

                override fun connectionLost(cause: Throwable?) {
                    postState("Mất kết nối MQTT")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString()?.trim() ?: return
                    val action = parseAction(payload)
                    if (action.isNotEmpty()) PlaybackCommandBus.emit(action)
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) {}
            })

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
                isAutomaticReconnect = true
                if (p.username.isNotEmpty()) {
                    userName = p.username
                    if (p.password.isNotEmpty()) this.password = p.password.toCharArray()
                }
            }
            c.connect(options)

            if (!shouldBeConnected) {
                try { c.disconnect() } catch (e: Exception) { }
                return
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            if (shouldBeConnected && attempt < MAX_ATTEMPTS) {
                postState("Lỗi kết nối MQTT (lần $attempt), tự thử lại...")
                delay(RETRY_DELAY_MS)
                attemptConnect(attempt + 1)
            } else {
                postState("Lỗi kết nối MQTT: ${e.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (shouldBeConnected && attempt < MAX_ATTEMPTS) {
                postState("Lỗi kết nối (lần $attempt), tự thử lại...")
                delay(RETRY_DELAY_MS)
                attemptConnect(attempt + 1)
            } else {
                postState("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun parseAction(payload: String): String {
        return try {
            val cmd = Gson().fromJson(payload, MqttCommand::class.java)
            cmd?.action?.trim()?.lowercase() ?: payload
        } catch (e: Exception) {
            payload.lowercase()
        }
    }

    fun disconnect() {
        shouldBeConnected = false
        connectParams = null
        CoroutineScope(Dispatchers.IO).launch {
            disconnectClientOnly()
            postState("Đã ngắt kết nối")
        }
    }

    private fun disconnectClientOnly() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        client = null
    }

    fun isConnected(): Boolean = client?.isConnected == true

    private fun postState(state: String) {
        mainHandler.post { onState?.invoke(state) }
    }
}
