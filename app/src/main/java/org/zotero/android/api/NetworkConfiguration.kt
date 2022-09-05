package org.zotero.android.api

import javax.inject.Inject

private const val NETWORK_TIMEOUT = 15L

class NetworkConfiguration @Inject constructor(
) : ApiConfiguration {
    override val networkTimeout: Long = NETWORK_TIMEOUT
}
