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

/** Must run after `ProjectAuthorizationInterceptor`, which sets [AppAuthentication.boundProjectId]. */
@Component
class AppAccessInterceptor(
  private val authenticationFacade: AuthenticationFacade,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    if (request.dispatcherType == DispatcherType.ERROR) return true
    if (!authenticationFacade.isAppAuth) {
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
