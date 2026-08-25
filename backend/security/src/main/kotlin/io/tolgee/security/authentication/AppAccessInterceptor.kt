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
    if (!authenticationFacade.isAppAuth) {
      // The app-only endpoints exist for app tokens; a non-app caller (session/PAK/PAT) is refused here.
      if (isAppOnly(handler)) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
      return true
    }
    if (hasAnnotation(handler, AppAccessNeutral::class.java)) return true
    if (hasAnnotation(handler, DenyAppAccess::class.java)) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    if (allowsMethod(handler, AllowAppLevelAccess::class.java)) {
      if (!authenticationFacade.appAuthentication.isAppLevel) {
        throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
      }
      return true
    }
    if (allowsMethod(handler, AllowAppOwnInstallAccess::class.java)) {
      // The annotation's contract is "derive the install from the token's own claims", which only an
      // install-context token honours; a user- or app-level token here would read installs it never owns.
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

  private fun isAppOnly(handler: HandlerMethod): Boolean =
    allowsMethod(handler, AllowAppLevelAccess::class.java) ||
      allowsMethod(handler, AllowAppOwnInstallAccess::class.java)

  private fun <A : Annotation> allowsMethod(
    handler: HandlerMethod,
    type: Class<A>,
  ): Boolean = AnnotationUtils.getAnnotation(handler.method, type) != null
}
