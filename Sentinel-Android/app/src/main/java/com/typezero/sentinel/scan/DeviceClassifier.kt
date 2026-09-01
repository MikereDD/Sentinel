package com.typezero.sentinel.scan

import com.typezero.sentinel.data.model.DeviceProfile
import com.typezero.sentinel.data.model.DeviceType

/**
 * Fuses the available signals into a [DeviceProfile]. No single signal is
 * authoritative, so this walks them strongest-first and returns the best match.
 *
 * Signal strength, roughly:
 *   gateway match / SSDP model > distinctive open ports > mDNS service types >
 *   hostname keywords.
 */
object DeviceClassifier {

    /** Ports worth probing per host; chosen so each maps to a fairly specific class. */
    val SIGNATURE_PORTS: List<Int> = listOf(
        22,     // SSH -> computer / server / Pi
        80, 443, // web UI -> router / IoT / NAS / printer
        139, 445, // SMB -> Windows / NAS
        3389,   // RDP -> Windows
        5000, 5001, // Synology / UPnP / misc
        515, 631, 9100, // LPD / IPP / raw printing -> printer
        8009, 8008, 8443, // Chromecast / Google Cast -> TV
        62078,  // lockdownd -> iOS phone/tablet
        32400,  // Plex -> media server
        1883,   // MQTT -> IoT hub
        8123,   // Home Assistant
        1400,   // Sonos
        554, 37777, // RTSP / Dahua -> camera
        8060,   // Roku ECP -> TV
        548,    // AFP -> Mac / Time Capsule
        5555    // ADB -> Android
    )

    fun classify(
        ip: String,
        gatewayIp: String?,
        openPorts: Set<Int>,
        mdnsServices: List<String>,
        ssdp: SsdpDiscovery.SsdpInfo?,
        hostname: String?,
        mac: String?
    ): DeviceProfile {
        // 1. The default gateway is the router, full stop.
        if (gatewayIp != null && ip == gatewayIp) {
            return DeviceProfile(
                DeviceType.ROUTER,
                vendor = ssdp?.manufacturer,
                model = ssdp?.modelName,
                confidence = 95,
                reason = "default gateway"
            )
        }

        // 2. Gather a candidate from every signal source and take the most confident.
        //    A confident hostname (e.g. "Mac mini") therefore beats an ambiguous mDNS
        //    hint (AirPlay -> TV), which fixes Macs being mislabelled as TVs.
        val candidates = listOfNotNull(
            ssdpProfile(ssdp),
            portProfile(openPorts),
            mdnsProfile(mdnsServices),
            hostnameProfile(hostname)
        )
        val best = candidates.maxByOrNull { it.confidence } ?: return DeviceProfile.UNKNOWN

        // Backfill brand/model from whichever candidate has them.
        return best.copy(
            vendor = best.vendor ?: candidates.firstNotNullOfOrNull { it.vendor },
            model = best.model ?: candidates.firstNotNullOfOrNull { it.model }
        )
    }

    private fun ssdpProfile(ssdp: SsdpDiscovery.SsdpInfo?): DeviceProfile? {
        if (ssdp == null) return null
        val blob = listOfNotNull(
            ssdp.deviceType, ssdp.modelName, ssdp.manufacturer,
            ssdp.friendlyName, ssdp.server
        ).joinToString(" ").lowercase()
        if (blob.isBlank() && ssdp.st.isEmpty()) return null

        val vendor = ssdp.manufacturer?.takeIf { it.isNotBlank() }
        val model = ssdp.modelName?.takeIf { it.isNotBlank() }
        val st = ssdp.st.joinToString(" ").lowercase()

        val type = when {
            "internetgatewaydevice" in st || "wandevice" in blob -> DeviceType.ROUTER
            listOf("xbox", "playstation", "ps4", "ps5", "nintendo").any { it in blob } ->
                DeviceType.GAME_CONSOLE
            listOf("samsung", "lg", "sony", "bravia", "webos", "vizio", "tcl",
                "roku", "firetv", "fire tv", "shield", "philips tv").any { it in blob } ->
                DeviceType.TV
            "mediarenderer" in st || "tv" in blob -> DeviceType.TV
            "sonos" in blob || "speaker" in blob || "audio" in blob -> DeviceType.SPEAKER
            "mediaserver" in st || "plex" in blob -> DeviceType.SERVER
            listOf("synology", "qnap", "nas", "diskstation").any { it in blob } -> DeviceType.NAS
            "printer" in blob -> DeviceType.PRINTER
            "camera" in blob -> DeviceType.CAMERA
            else -> DeviceType.UNKNOWN
        }
        if (type == DeviceType.UNKNOWN && vendor == null) return null
        return DeviceProfile(
            type = if (type == DeviceType.UNKNOWN) DeviceType.COMPUTER else type,
            vendor = vendor,
            model = model,
            confidence = if (type == DeviceType.UNKNOWN) 55 else 90,
            reason = "SSDP" + (vendor?.let { ": $it" } ?: "")
        )
    }

    private fun portProfile(ports: Set<Int>): DeviceProfile? {
        if (ports.isEmpty()) return null
        fun p(vararg x: Int) = x.any { it in ports }
        return when {
            p(62078) -> DeviceProfile(DeviceType.PHONE, "Apple", null, 90, "port 62078 (iOS)")
            p(8009, 8008, 8443) -> DeviceProfile(DeviceType.TV, null, null, 85, "Chromecast/Google Cast")
            p(8060) -> DeviceProfile(DeviceType.TV, "Roku", null, 85, "Roku ECP")
            p(631, 9100, 515) -> DeviceProfile(DeviceType.PRINTER, null, null, 88, "printing ports")
            p(1400) -> DeviceProfile(DeviceType.SPEAKER, "Sonos", null, 85, "Sonos")
            p(554, 37777) -> DeviceProfile(DeviceType.CAMERA, null, null, 80, "RTSP")
            p(32400) -> DeviceProfile(DeviceType.SERVER, null, null, 80, "Plex media server")
            p(8123) -> DeviceProfile(DeviceType.SERVER, null, "Home Assistant", 75, "Home Assistant")
            p(1883) -> DeviceProfile(DeviceType.IOT, null, null, 70, "MQTT broker")
            p(3389) -> DeviceProfile(DeviceType.COMPUTER, "Microsoft", null, 85, "RDP (Windows)")
            p(5000, 5001) && p(445, 139) -> DeviceProfile(DeviceType.NAS, null, null, 75, "SMB + Synology ports")
            p(548) -> DeviceProfile(DeviceType.COMPUTER, "Apple", null, 70, "AFP (Mac)")
            p(445, 139) -> DeviceProfile(DeviceType.COMPUTER, null, null, 65, "SMB file sharing")
            p(5555) -> DeviceProfile(DeviceType.PHONE, null, null, 60, "ADB (Android)")
            p(22) -> DeviceProfile(DeviceType.COMPUTER, null, null, 60, "SSH (computer/server)")
            else -> null
        }
    }

    private fun mdnsProfile(services: List<String>): DeviceProfile? {
        if (services.isEmpty()) return null
        val s = services.joinToString(" ").lowercase()
        return when {
            "googlecast" in s || "androidtvremote" in s -> DeviceProfile(DeviceType.TV, "Google", null, 85, "mDNS cast/Android TV")
            "amzn-wplay" in s -> DeviceProfile(DeviceType.TV, "Amazon", null, 85, "mDNS Fire TV")
            "airplay" in s || "raop" in s -> DeviceProfile(DeviceType.TV, "Apple", null, 70, "mDNS AirPlay")
            "spotify-connect" in s || "sonos" in s -> DeviceProfile(DeviceType.SPEAKER, null, null, 75, "mDNS audio")
            "printer" in s || "ipp" in s || "pdl-datastream" in s -> DeviceProfile(DeviceType.PRINTER, null, null, 85, "mDNS printer")
            "homekit" in s || "hap" in s || "hue" in s -> DeviceProfile(DeviceType.IOT, null, null, 75, "mDNS HomeKit/Hue")
            "smb" in s || "afpovertcp" in s -> DeviceProfile(DeviceType.NAS, null, null, 60, "mDNS file sharing")
            "companion-link" in s || "apple-mobdev" in s -> DeviceProfile(DeviceType.PHONE, "Apple", null, 60, "mDNS Apple device")
            "ssh" in s || "sftp-ssh" in s -> DeviceProfile(DeviceType.COMPUTER, null, null, 55, "mDNS SSH")
            "workstation" in s -> DeviceProfile(DeviceType.COMPUTER, null, null, 55, "mDNS workstation")
            else -> null
        }
    }

    private fun hostnameProfile(hostname: String?): DeviceProfile? {
        val h = hostname?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        fun has(vararg k: String) = k.any { it in h }
        return when {
            has("iphone", "ipad", "ipod") -> DeviceProfile(DeviceType.PHONE, "Apple", null, 80, "hostname")
            has("macbook", "imac", "mac mini", "mac-mini", "macmini", "macpro") -> DeviceProfile(DeviceType.COMPUTER, "Apple", null, 85, "hostname")
            has("blink", "doorbell", "camera", "ipcam", "reolink", "arlo", "wyze", "ring-") -> DeviceProfile(DeviceType.CAMERA, null, null, 75, "hostname")
            has("android", "galaxy", "pixel", "oneplus", "redmi", "xiaomi") -> DeviceProfile(DeviceType.PHONE, null, null, 65, "hostname")
            has("desktop-", "laptop-", "win-", "windows") -> DeviceProfile(DeviceType.COMPUTER, "Microsoft", null, 65, "hostname")
            has("raspberrypi", "ubuntu", "debian", "arch", "fedora") -> DeviceProfile(DeviceType.COMPUTER, null, null, 60, "hostname")
            has("bravia", "samsung", "webos", "roku", "firetv", "shield", "chromecast", "smarttv") -> DeviceProfile(DeviceType.TV, null, null, 65, "hostname")
            has("esp_", "esp-", "tasmota", "shelly", "sonoff", "tuya") -> DeviceProfile(DeviceType.IOT, null, null, 65, "hostname")
            has("sonos", "echo", "alexa", "homepod") -> DeviceProfile(DeviceType.SPEAKER, null, null, 65, "hostname")
            has("synology", "qnap", "nas", "diskstation") -> DeviceProfile(DeviceType.NAS, null, null, 65, "hostname")
            has("printer", "epson", "canon", "brother", "officejet") -> DeviceProfile(DeviceType.PRINTER, null, null, 65, "hostname")
            else -> null
        }
    }
}
