package io.polygloat.configuration.polygloat

import io.polygloat.constants.Message
import io.polygloat.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "polygloat.authentication")
class AuthenticationProperties(
        var enabled: Boolean = true,
        var jwtSecret: String? = null,
        var jwtExpiration: Int = 604800000,
        var nativeEnabled: Boolean = true,
        var registrationsAllowed: Boolean = false,
        var createInitialUser: Boolean = true,
        var initialUsername: String = "admin",
        var github: GithubAuthenticationProperties = GithubAuthenticationProperties(),
        var ldap: LdapAuthenticationProperties = LdapAuthenticationProperties()
) {
    fun checkAllowedRegistrations() {
        if (!this.registrationsAllowed) {
            throw BadRequestException(Message.REGISTRATIONS_NOT_ALLOWED)
        }
    }
}