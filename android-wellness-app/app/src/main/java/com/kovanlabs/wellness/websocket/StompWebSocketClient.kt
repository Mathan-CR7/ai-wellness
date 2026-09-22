package com.kovanlabs.wellness.websocket

import android.util.Log
import com.google.gson.Gson
import com.kovanlabs.wellness.model.InactivitySuggestionDto
import okhttp3.*
import okio.ByteString

class StompWebSocketClient(private val listener: (InactivitySuggestionDto) -> Unit) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    fun connect(url: String = "wss://ai-wellness-jt1d.onrender.com/ws/websocket") {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("STOMP", "WebSocket connected to $url")
                // STOMP CONNECT frame
                val connectFrame = "CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\u0000"
                ws.send(connectFrame)

                // STOMP SUBSCRIBE frame to /topic/inactivity-suggestions
                val subFrame = "SUBSCRIBE\nid:sub-0\ndestination:/topic/inactivity-suggestions\n\n\u0000"
                ws.send(subFrame)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                if (text.startsWith("MESSAGE")) {
                    try {
                        val bodyStart = text.indexOf("\n\n")
                        if (bodyStart != -1) {
                            val body = text.substring(bodyStart + 2).trimEnd('\u0000')
                            val suggestion = gson.fromJson(body, InactivitySuggestionDto::class.java)
                            if (suggestion != null) {
                                listener(suggestion)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("STOMP", "Error parsing STOMP message: ${e.message}")
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w("STOMP", "WebSocket failure: ${t.message}")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
    }
}
