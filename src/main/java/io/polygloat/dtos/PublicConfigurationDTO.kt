package io.polygloat.dtos

import io.polygloat.configuration.polygloat.PolygloatProperties

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(properties: PolygloatProperties) {
    val isAuthentication: Boolean = properties.authentication.enabled
    var authMethods: AuthMethodsDTO? = null
    val isPasswordResettable: Boolean
    val isAllowRegistrations: Boolean
    val screenshotsUrl = properties.screenshotsUrl
    val maxUploadFileSize = properties.maxUploadFileSize
    val clientSentryDsn = if (properties.sentry.enabled) properties.sentry.clientDsn else null
    val needsEmailVerification = properties.authentication.needsEmailVerification

    class AuthMethodsDTO(val github: GithubPublicConfigDTO)
    class GithubPublicConfigDTO(val clientId: String?) {
        val isEnabled: Boolean = clientId != null && clientId.isNotEmpty()
    }

    init {
        if (isAuthentication) {
            authMethods = AuthMethodsDTO(GithubPublicConfigDTO(properties.authentication.github.clientId))
        }
        isPasswordResettable = properties.authentication.nativeEnabled
        isAllowRegistrations = properties.authentication.registrationsAllowed
    }
}