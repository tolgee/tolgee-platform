package io.tolgee.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

/**
 * The bridge from the stateless webapp JWT to the browser session `/oauth2/authorize` needs: a top-level navigation
 * carries no `Authorization` header, so the bootstrap page turns the stored JWT into a short-lived server session
 * first. The session holds nothing but the user id, and only the authorization endpoint reads it.
 */
@Component
class OAuth2SessionBootstrapper {
  fun establishSession(
    request: HttpServletRequest,
    userId: Long,
  ) {
    request.getSession(true)
    // Session-fixation defense: the id the browser held before it carried a principal must not survive.
    request.changeSessionId()
    request.session.setAttribute(USER_ID_ATTRIBUTE, userId)
  }

  fun userIdOf(request: HttpServletRequest): Long? = request.getSession(false)?.getAttribute(USER_ID_ATTRIBUTE) as? Long

  companion object {
    const val USER_ID_ATTRIBUTE = "tolgee.oauth2.userId"
  }
}
