package io.tolgee.security

import io.tolgee.model.UserAccount
import io.tolgee.service.UserAccountService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import java.util.*

@Component
class AuthenticationProvider(
  private val userAccountService: UserAccountService
) {
  fun getAuthentication(userAccount: UserAccount): Authentication {
    val userDetails = userAccount
    val authorities: MutableList<GrantedAuthority> = LinkedList()
    val grantedAuthority = GrantedAuthority { "user" }
    authorities.add(grantedAuthority)
    return UsernamePasswordAuthenticationToken(userDetails, null, authorities)
  }
}
