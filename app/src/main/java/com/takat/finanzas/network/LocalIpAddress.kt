package com.takat.finanzas.network

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Best-effort LAN IPv4 address, e.g. to show "conéctate a http://192.168.1.42:8765" in Settings.
 * Uses NetworkInterface enumeration instead of the deprecated WifiManager APIs (which also need
 * location permission on modern Android) — works the same whether the phone is on wifi or a
 * wifi-hotspot-style LAN, which is all this feature needs.
 */
object LocalIpAddress {
    fun current(): String? =
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.filter { !it.isLoopback && it.isUp }
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull()
            ?.hostAddress
}
