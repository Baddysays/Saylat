package com.baddysays.saylat.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
/** Определяет реально медленный канал (2G/EDGE), даже если режим 2G в настройках выключен. */
object NetworkLinkSpeed {

    fun isSlowCellular(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return false
        val kbps = caps.linkDownstreamBandwidthKbps
        return kbps in 1..500
    }
}
