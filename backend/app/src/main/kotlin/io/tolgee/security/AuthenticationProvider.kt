package io.tolgee.security

import io.tolgee.dtos.cacheable.UserAccountDto
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import java.util.*

@Component
class AuthenticationProvider {
  fun getAuthentication(userAccount: UserAccountDto): Authentication {
    val authorities: MutableList<GrantedAuthority> = LinkedList()
    val grantedAuthority = GrantedAuthority { "user" }
    authorities.add(grantedAuthority)
    return UsernamePasswordAuthenticationToken(userAccount, null, authorities)
  }
}
