package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygloat.authentication.ldap")
class LdapAuthenticationProperties {
    var enabled: Boolean = false;

    var port: String? = null

    var urls: String? = null

    var baseDn: String? = null

    var securityPrincipal: String? = null

    var principalPassword: String? = null

    var userDnPattern: String? = null
}
