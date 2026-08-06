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
 * An app token carries the identity of the install's author (typically an organization owner), and
 * only the project-scoped path caps it to the install's granted scopes — via
 * [io.tolgee.security.ProjectContextService], which is also the only thing that sets
 * [AppAuthentication.boundProjectId]. Anywhere else the token would act with the author's own
 * privileges, so it is rejected.
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
    if (deniesAppAccess(handler)) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    if (authenticationFacade.appAuthentication.boundProjectId != null) return true
    throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
  }

  private fun deniesAppAccess(handler: HandlerMethod): Boolean {
    if (AnnotationUtils.getAnnotation(handler.method, DenyAppAccess::class.java) != null) return true
    return AnnotationUtils.findAnnotation(handler.beanType, DenyAppAccess::class.java) != null
  }
}
