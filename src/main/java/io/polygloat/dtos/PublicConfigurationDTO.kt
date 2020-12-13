package io.polygloat.dtos

import io.polygloat.configuration.polygloat.PolygloatProperties

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(configuration: PolygloatProperties) {
    val isAuthentication: Boolean = configuration.authentication.enabled
    var authMethods: AuthMethodsDTO? = null
    val isPasswordResettable: Boolean
    val isAllowRegistrations: Boolean
    val screenshotsUrl = configuration.screenshotsUrl
    val maxUploadFileSize = configuration.maxUploadFileSize

    class AuthMethodsDTO(val github: GithubPublicConfigDTO)
    class GithubPublicConfigDTO(val clientId: String?) {
        val isEnabled: Boolean = clientId != null && clientId.isNotEmpty()
    }

    init {
        if (isAuthentication) {
            authMethods = AuthMethodsDTO(GithubPublicConfigDTO(configuration.authentication.github.clientId))
        }
        isPasswordResettable = configuration.authentication.nativeEnabled
        isAllowRegistrations = configuration.authentication.registrationsAllowed
    }
}