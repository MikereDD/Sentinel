package com.typezero.sentinel.data.model

/**
 * Coarse device category inferred from network signals. This is a *category*,
 * not a guaranteed OS — exact OS fingerprinting needs raw sockets/root, which an
 * unprivileged Android app can't do. Categories (and often brand/model) are very
 * achievable from open ports, SSDP/UPnP, mDNS, and hostname hints.
 */
enum class DeviceType {
    PHONE,
    COMPUTER,
    TV,
    ROUTER,
    NAS,
    PRINTER,
    IOT,
    SPEAKER,
    CAMERA,
    SERVER,
    GAME_CONSOLE,
    UNKNOWN;

    val pretty: String
        get() = when (this) {
            PHONE -> "Phone / Tablet"
            COMPUTER -> "Computer"
            TV -> "TV / Streaming"
            ROUTER -> "Router / Gateway"
            NAS -> "NAS / Storage"
            PRINTER -> "Printer"
            IOT -> "Smart home"
            SPEAKER -> "Speaker"
            CAMERA -> "Camera"
            SERVER -> "Server"
            GAME_CONSOLE -> "Game console"
            UNKNOWN -> "Unknown"
        }
}

/**
 * Result of classification.
 * @param confidence 0-100; UNKNOWN is always 0.
 * @param reason short human-readable evidence ("SSDP: Samsung", "port 62078 (iOS)").
 */
data class DeviceProfile(
    val type: DeviceType,
    val vendor: String?,
    val model: String?,
    val confidence: Int,
    val reason: String?
) {
    companion object {
        val UNKNOWN = DeviceProfile(DeviceType.UNKNOWN, null, null, 0, null)
    }
}
