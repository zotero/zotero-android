package org.zotero.android.architecture

/**
 * Data class representing info about the app's current environment.
 *
 * @param domain The base url used for interacting with backend services. This is non-user facing.
 * @param inviteLinkRoot A user facing URL used for invites and shareable deep link content.
 */
data class DeploymentEnvironment(
    val domain: String,
    val inviteLinkRoot: String
)
