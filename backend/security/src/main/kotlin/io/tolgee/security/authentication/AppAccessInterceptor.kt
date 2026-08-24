package io.tolgee.security.authentication

import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authorization.AbstractAuthorizationInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * Denies app tokens everywhere except the project-scoped routes they exist for.
 *
 * Only the project-scoped path caps an app token to the install's granted scopes — via
 * [io.tolgee.security.ProjectContextService], which is also the only thing that sets
 * [AppAuthentication.boundProjectId]. Anywhere else the token would reach an endpoint that was
 * written for a signed-in person, so it is rejected.
 *
 * Two exceptions: [AllowAppOwnInstallAccess], for endpoints that only ever report on the caller's
 * own install; and [AppAccessNeutral], for the credential-authenticated `/v2/public/apps` routes
 * that must not be denied merely because the caller also sent a bearer token.
 *
 * Must be registered after `ProjectAuthorizationInterceptor`.
 */
@Component
class AppAccessInterceptor(
  private val authenticationFacade: AuthenticationFacade,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    if (!authenticationFacade.isAppAuth) return true
    if (isAppAccessNeutral(handler)) return true
    if (deniesAppAccess(handler)) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    if (allowsOwnInstallAccess(handler)) {
      // The annotation's contract is "derive the install from the token's own claims", which only an
      // install-context token honours; a user-context token here would read installs it never owns.
      if (!authenticationFacade.appAuthentication.isInstallContext) {
        throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
      }
      return true
    }
    if (authenticationFacade.appAuthentication.boundProjectId != null) return true
    throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
  }

  private fun isAppAccessNeutral(handler: HandlerMethod): Boolean {
    if (AnnotationUtils.getAnnotation(handler.method, AppAccessNeutral::class.java) != null) return true
    return AnnotationUtils.findAnnotation(handler.beanType, AppAccessNeutral::class.java) != null
  }

  private fun deniesAppAccess(handler: HandlerMethod): Boolean {
    if (AnnotationUtils.getAnnotation(handler.method, DenyAppAccess::class.java) != null) return true
    return AnnotationUtils.findAnnotation(handler.beanType, DenyAppAccess::class.java) != null
  }

  private fun allowsOwnInstallAccess(handler: HandlerMethod): Boolean {
    return AnnotationUtils.getAnnotation(handler.method, AllowAppOwnInstallAccess::class.java) != null
  }
}
