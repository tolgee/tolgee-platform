package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.ldap")
@DocProperty(description = ":::warning\n" +
  "This feature is experimental!\n" +
  ":::\n" +
  "\n" +
  ":::info\n" +
  "LDAP authentication can be used in combination with GitHub or native authentication.\n" +
  ":::\n" +
  "\n" +
  "Tolgee can use a LDAP server to authenticate users. This is very useful if you already use LDAP as a primary mean of\n" +
  "authentication for other services.",
  displayName = "LDAP")
class LdapAuthenticationProperties {
  @DocProperty(description = "Whether LDAP authentication is enabled. If enabled, you need to set all remaining properties below.")
  var enabled: Boolean = false

  @DocProperty(description = "LDAP server host")
  var port: String? = null

  @DocProperty(description = "LDAP server URLs. For example `ldap://localhost:389`")
  var urls: String? = null

  @DocProperty(description = "LDAP base DN. For example `dc=example,dc=com`")
  var baseDn: String? = null

  @DocProperty(description = "LDAP manager DN. For example `cn=admin,dc=example,dc=com`")
  var securityPrincipal: String? = null

  @DocProperty(description = "LDAP password for the manager DN")
  var principalPassword: String? = null

  @DocProperty(description = "LDAP user search filter. For example `(uid={0})`")
  var userDnPattern: String? = null
}
