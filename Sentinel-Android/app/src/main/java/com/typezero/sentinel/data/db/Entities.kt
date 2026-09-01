package com.typezero.sentinel.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A device Sentinel has learned about on a given network.
 *
 * Identity is keyed by [identityKey], which is MAC-based when available
 * ([strongIdentity] == true) and otherwise a best-effort fingerprint.
 * Devices are scoped to [networkKey] so foreign networks (e.g. a cafe Wi-Fi)
 * never pollute the home device list.
 */
@Entity(
    tableName = "known_devices",
    indices = [Index(value = ["networkKey", "identityKey"], unique = true)]
)
data class KnownDevice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val networkKey: String,
    val identityKey: String,
    val displayName: String,
    val lastIp: String,
    val macAddress: String?,
    val hostname: String?,
    val services: String,
    val strongIdentity: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
    val online: Boolean,
    /** Consecutive discovery scans in which this device was not seen. */
    val missedScans: Int = 0,
    val deviceType: String = "UNKNOWN",
    val vendor: String? = null,
    val model: String? = null,
    val typeConfidence: Int = 0,
    /** User-set name; overrides [displayName] in the UI and is never touched by scans. */
    val customName: String? = null,
    /** User-set [com.typezero.sentinel.data.model.DeviceType] name; overrides auto typing. */
    val userType: String? = null,
    /** User-set room/location label, e.g. "Living room". Null = unassigned. */
    val room: String? = null
)

/**
 * A user-pinned target whose up/down state Sentinel watches directly
 * (the Pi, the Eero gateway, Home Assistant, etc.). Reachability of these
 * does not depend on device discovery, so their events are reliable.
 */
@Entity(tableName = "watched_targets")
data class WatchedTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val host: String,
    /** null -> ICMP/ping check; non-null -> TCP connect to this port. */
    val port: Int?,
    /** null = never checked yet. */
    val lastOnline: Boolean?,
    val lastChecked: Long
)

/** A single entry in the change timeline. */
@Entity(tableName = "network_events")
data class NetworkEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val title: String,
    val detail: String?,
    val networkKey: String
)

/** Per-network rollup of infra state, used to diff internet/gateway across scans. */
@Entity(tableName = "network_state")
data class NetworkState(
    @PrimaryKey val networkKey: String,
    val internetUp: Boolean,
    val gatewayUp: Boolean,
    val lastScan: Long
)
