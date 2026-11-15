package com.dcnm.donghaeng.vehicle

enum class UxMode { Unknown, Restricted, Unrestricted }

/** Unknown is fail-closed. A preview never overrides platform restrictions. */
fun canConfigure(mode: UxMode) = mode == UxMode.Unrestricted

enum class NearbyRequest { None, Outgoing, Incoming, Connected, Invalid, Blocked }
data class NearbyCandidate(val id: String, val name: String, val destinationId: String, val distanceM: Int)
data class NearbyPreview(
    val enabled: Boolean = false,
    val radiusM: Int = 500,
    val destinationId: String? = null,
    val request: NearbyRequest = NearbyRequest.None,
    val blocked: Set<String> = emptySet(),
) {
    fun candidates(all: List<NearbyCandidate>) = if (!enabled || destinationId == null) emptyList() else
        all.filter { it.destinationId == destinationId && it.distanceM <= radiusM && it.id !in blocked }
    fun stop() = copy(enabled = false, request = NearbyRequest.None)
    fun destination(id: String?) = copy(destinationId = id, enabled = false, request = NearbyRequest.None)
    fun radius(value: Int) = copy(radiusM = value, request = NearbyRequest.None)
    fun send() = if (enabled && destinationId != null) copy(request = NearbyRequest.Outgoing) else this
    fun receive() = if (enabled && destinationId != null) copy(request = NearbyRequest.Incoming) else this
    fun acceptIncoming() = if (enabled && request == NearbyRequest.Incoming) copy(request = NearbyRequest.Connected) else this
    fun peerAccepts() = if (enabled && request == NearbyRequest.Outgoing) copy(request = NearbyRequest.Connected) else this
    fun block(id: String) = copy(blocked = blocked + id, request = NearbyRequest.Blocked)
}
