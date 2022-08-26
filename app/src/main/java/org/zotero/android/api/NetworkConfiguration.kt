package org.zotero.android.api

import org.zotero.android.architecture.DeploymentEnvironment
import org.zotero.android.architecture.app.AppVersionProvider
import javax.inject.Inject

private const val NETWORK_TIMEOUT = 15L

class NetworkConfiguration @Inject constructor(
    environment: DeploymentEnvironment,
    appVersionProvider: AppVersionProvider,
) : ApiConfiguration, SvcSocketConfiguration {
    override val appVersion: String = appVersionProvider.provide()
    override val domain: String = environment.domain
    override val networkTimeout: Long = NETWORK_TIMEOUT
}
