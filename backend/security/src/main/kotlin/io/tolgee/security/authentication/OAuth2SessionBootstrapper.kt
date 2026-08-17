package io.tolgee.security.authentication

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component

@Component
class OAuth2SessionBootstrapper {
  fun establishSession(
    request: HttpServletRequest,
    userId: Long,
  ) {
    // Must be a built-in Spring Security auth type: SAS persists the principal via a whitelist Jackson mapper that
    // rejects Tolgee's own auth classes. The name (= user id) becomes the token `sub` and stored `principal_name`.
    val authentication = UsernamePasswordAuthenticationToken(userId.toString(), null, AuthorityUtils.NO_AUTHORITIES)
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = authentication
    request.getSession(true)
    // Manual session-fixation defense: Spring's built-in one doesn't run for a manually-injected principal.
    request.changeSessionId()
    request.session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)
  }
}
