package io.tolgee.security.authentication

import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authorization.AbstractAuthorizationInterceptor
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * Denies app tokens everywhere except the project-scoped routes that cap them to the install's
 * granted scopes (via [io.tolgee.security.ProjectContextService], which sets [AppAuthentication.boundProjectId]).
 * Exceptions: [AllowAppOwnInstallAccess] and [AppAccessNeutral]. Must run after `ProjectAuthorizationInterceptor`.
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
    // The error dispatch re-enters here with nothing bound; leave the original error response intact.
    if (request.dispatcherType == DispatcherType.ERROR) return true
    if (!authenticationFacade.isAppAuth) return true
    if (hasAnnotation(handler, AppAccessNeutral::class.java)) return true
    if (hasAnnotation(handler, DenyAppAccess::class.java)) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
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

  private fun <A : Annotation> hasAnnotation(
    handler: HandlerMethod,
    type: Class<A>,
  ): Boolean {
    if (AnnotationUtils.getAnnotation(handler.method, type) != null) return true
    return AnnotationUtils.findAnnotation(handler.beanType, type) != null
  }

  private fun allowsOwnInstallAccess(handler: HandlerMethod): Boolean {
    return AnnotationUtils.getAnnotation(handler.method, AllowAppOwnInstallAccess::class.java) != null
  }
}
