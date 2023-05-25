package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.ldap")
@DocProperty(description = "LDAP authentication settings")
class LdapAuthenticationProperties {
  var enabled: Boolean = false

  var port: String? = null

  var urls: String? = null

  var baseDn: String? = null

  var securityPrincipal: String? = null

  var principalPassword: String? = null

  var userDnPattern: String? = null
}
