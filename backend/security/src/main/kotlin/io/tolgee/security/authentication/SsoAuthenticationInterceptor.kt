package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.TenantService
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SsoAuthenticationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val tolgeeProperties: TolgeeProperties,
  @Lazy
  private val tenantService: TenantService,
) : HandlerInterceptor,
  Ordered {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }

    if (request.method == "OPTIONS") {
      // Do not process OPTIONS requests
      return true
    }

    if (!authenticationFacade.isAuthenticated) {
      // Allow unauthenticated requests - not our concern
      return super.preHandle(request, response, handler)
    }

    val user = authenticationFacade.authenticatedUser
    checkNonSsoAccessAllowed(user, handler)

    return true
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  private fun checkNonSsoAccessAllowed(
    userAccount: UserAccountDto,
    handler: HandlerMethod,
  ) {
    if (handler.hasMethodAnnotation(BypassForcedSsoAuthentication::class.java)) {
      return
    }

    if (!tolgeeProperties.authentication.enabled) {
      return
    }

    val isSsoLocal = userAccount.thirdPartyAuth == ThirdPartyAuthType.SSO
    val isSsoGlobal = userAccount.thirdPartyAuth == ThirdPartyAuthType.SSO_GLOBAL
    if (isSsoGlobal || isSsoLocal) {
      // Already an SSO account
      return
    }

    if (!tenantService.isSsoForcedForDomain(userAccount.domain)) {
      return
    }

    throw PermissionException(Message.SSO_LOGIN_FORCED_FOR_THIS_ACCOUNT, listOf(userAccount.domain))
  }
}
