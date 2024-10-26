package io.tolgee.model.enums

enum class ThirdPartyAuthType {
  GOOGLE,
  GITHUB,
  OAUTH2,
  SSO,
  ;

  fun code(): String = name.lowercase()
}
