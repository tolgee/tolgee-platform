package io.tolgee.security.apiKeyAuth

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.ApiKey
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class ApiKeyAuthenticationToken(val apiKey: ApiKey) : UsernamePasswordAuthenticationToken(
  UserAccountDto.fromEntity(
    apiKey.userAccount
  ),
  null,
  setOf(
    GrantedAuthority { "api" }
  )
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ApiKeyAuthenticationToken

    if (apiKey != other.apiKey) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + apiKey.hashCode()
    return result
  }
}
