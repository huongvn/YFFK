package com.example.myapplication.mqtt

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

object MqttController {

    private var client: MqttClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    var onState: ((String) -> Unit)? = null

    fun connect(broker: String, clientId: String, topic: String, username: String = "", password: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                disconnect()
                val c = MqttClient(broker, clientId, MemoryPersistence())
                client = c
                c.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
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
                    if (username.isNotEmpty()) {
                        userName = username
                        if (password.isNotEmpty()) this.password = password.toCharArray()
                    }
                }
                c.connect(options)
                c.subscribe(topic, 1)
                postState("Đã kết nối: $broker")
            } catch (e: MqttException) {
                e.printStackTrace()
                postState("Lỗi MQTT: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                postState("Lỗi: ${e.message}")
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client?.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            client = null
            postState("Đã ngắt kết nối")
        }
    }

    fun isConnected(): Boolean = client?.isConnected == true

    private fun postState(state: String) {
        mainHandler.post { onState?.invoke(state) }
    }
}
