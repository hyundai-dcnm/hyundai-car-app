package com.dcnm.donghaeng.core.network

import com.dcnm.donghaeng.core.model.TripEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * 여행방 WebSocket. 끊기면 1, 2, 4 ... 최대 15초 간격으로 다시 붙는다.
 * 다시 붙으면 서버가 snapshot 을 먼저 보내므로 끊긴 동안 놓친 이벤트는 따로 맞추지 않는다.
 */
internal class TripSocket(
    private val client: OkHttpClient,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TripEvent> = _events

    private val _status = MutableStateFlow(ConnectionStatus.Disconnected)
    val status: StateFlow<ConnectionStatus> = _status

    private var job: Job? = null
    private var socket: WebSocket? = null

    fun open(url: String) {
        close()
        job = scope.launch {
            var backoffMs = 1_000L
            while (true) {
                val closed = Channel<Unit>(Channel.CONFLATED)
                _status.value = if (_status.value == ConnectionStatus.Disconnected) ConnectionStatus.Connecting
                else ConnectionStatus.Reconnecting
                socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _status.value = ConnectionStatus.Connected
                        backoffMs = 1_000L
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runCatching { json.decodeFromString<TripEvent>(text) }.onSuccess { _events.tryEmit(it) }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        closed.trySend(Unit)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        closed.trySend(Unit)
                    }
                })
                closed.receive()
                _status.value = ConnectionStatus.Reconnecting
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(15_000L)
            }
        }
    }

    /** 연결이 없으면 버린다. 위치는 다음 갱신이 곧 오므로 다시 보내지 않는다. */
    fun send(event: TripEvent): Boolean = socket?.send(json.encodeToString(TripEvent.serializer(), event)) ?: false

    fun close() {
        job?.cancel()
        job = null
        socket?.close(1000, null)
        socket = null
        _status.value = ConnectionStatus.Disconnected
    }
}
