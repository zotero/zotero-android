package org.zotero.android.api.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

internal val ConnectivityManager.internetConnectionStatus: InternetConnectionStatus
    get() {
        val network: Network? = activeNetwork
        return if (network == null) {
            InternetConnectionStatus.DISCONNECTED
        } else {
            val capabilities = getNetworkCapabilities(network)
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                InternetConnectionStatus.CONNECTED
            } else {
                InternetConnectionStatus.DISCONNECTED
            }
        }
    }
