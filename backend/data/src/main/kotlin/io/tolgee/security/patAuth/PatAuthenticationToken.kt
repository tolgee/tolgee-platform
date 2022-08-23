package io.tolgee.security.patAuth

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.Pat
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class PatAuthenticationToken(val pat: Pat) : UsernamePasswordAuthenticationToken(
  UserAccountDto.fromEntity(
    pat.userAccount
  ),
  null,
  setOf(
    GrantedAuthority { "api" }
  )
)
