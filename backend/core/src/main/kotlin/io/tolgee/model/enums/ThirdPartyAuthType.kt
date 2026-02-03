package io.tolgee.model.enums

enum class ThirdPartyAuthType {
  GOOGLE,
  GITHUB,
  OAUTH2,
  SSO,
  SSO_GLOBAL,
  ;

  fun code(): String = name.lowercase()
}
