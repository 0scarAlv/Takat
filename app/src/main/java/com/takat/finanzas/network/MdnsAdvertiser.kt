package com.takat.finanzas.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.takat.finanzas.util.DebugLog

/**
 * Advertises the PC-access panel as `_takat._tcp` on the LAN via Android's NsdManager (mDNS/DNS-SD).
 *
 * Important limitation: this makes the phone *discoverable by name* to anything that does proper
 * DNS-SD service browsing (a future companion desktop helper, `dns-sd -B _takat._tcp` on macOS,
 * `avahi-browse` on Linux). It does **not** let someone type `http://takat-oscar.local:8765`
 * straight into Chrome/Edge on Windows — that needs the phone to answer plain A-record lookups for
 * that literal hostname, and Android's public API gives a normal app no way to claim the device's
 * own mDNS hostname. So the IP address shown in Settings stays the reliable way to connect; this
 * registration is a (currently unused) building block for a nicer discovery UX later, not a
 * replacement for it.
 */
class MdnsAdvertiser(context: Context) {
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null

    fun start(nickname: String?, port: Int) {
        stop()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = serviceNameFor(nickname)
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                DebugLog.log("MdnsAdvertiser: registered as ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                DebugLog.log("MdnsAdvertiser: registration failed ($errorCode)")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                DebugLog.log("MdnsAdvertiser: unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        listener = registrationListener
        runCatching { nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener) }
            .onFailure { DebugLog.log("MdnsAdvertiser: registerService threw ${it.message}") }
    }

    fun stop() {
        listener?.let { l -> runCatching { nsdManager.unregisterService(l) } }
        listener = null
    }

    companion object {
        private const val SERVICE_TYPE = "_takat._tcp"

        fun serviceNameFor(nickname: String?): String =
            if (nickname.isNullOrBlank()) "Takat" else "Takat - ${nickname.trim()}"
    }
}
