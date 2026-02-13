package io.tolgee.mcp

import io.tolgee.activity.ActivityHandlerInterceptor
import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectContextService
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.AuthenticationInterceptor
import io.tolgee.security.authentication.ReadOnlyModeInterceptor
import io.tolgee.security.authorization.FeatureAuthorizationInterceptor
import io.tolgee.security.authorization.FeatureCheckService
import io.tolgee.security.authorization.OrganizationAuthorizationInterceptor
import io.tolgee.security.ratelimit.RateLimitInterceptor
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.service.organization.OrganizationRoleService
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class McpRequestContext(
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val organizationHolder: OrganizationHolder,
  private val activityHolder: ActivityHolder,
  private val featureCheckService: FeatureCheckService,
  private val rateLimitService: RateLimitService,
  private val projectContextService: ProjectContextService,
) {
  fun <T> executeAs(
    spec: ToolEndpointSpec,
    projectId: Long? = null,
    block: () -> T,
  ): T {
    // Order mirrors the HTTP interceptor chain:
    // 1. RateLimitInterceptor
    applyRateLimit(spec)
    // 2. AuthenticationInterceptor (token type check)
    checkTokenType(spec)
    // 3–4. Reuses [ProjectContextService.setup], mirrors [ProjectAuthorizationInterceptor.preHandleInternal]
    if (!spec.isGlobalRoute) {
      if (projectId == null) throw ProjectNotSelectedException()
      projectContextService.setup(
        projectId,
        spec.requiredScopes,
        spec.useDefaultPermissions,
        spec.isWriteOperation,
      )
    }
    checkOrgRole(spec)
    // 5. ReadOnlyModeInterceptor
    checkReadOnlyMode(spec)
    // 6. FeatureAuthorizationInterceptor
    checkFeatures(spec)
    // 7. ActivityHandlerInterceptor (sets activity type + business event metadata)
    setupActivity(spec)
    emitPostHogEvent(spec)
    return block()
  }

  /** Mirrors [AuthenticationInterceptor.preHandle] */
  private fun checkTokenType(spec: ToolEndpointSpec) {
    if (!authenticationFacade.isApiAuthentication) return

    when (spec.allowedTokenType) {
      AuthTokenType.ONLY_PAT -> {
        if (authenticationFacade.isProjectApiKeyAuth) {
          throw PermissionException(Message.PAK_ACCESS_NOT_ALLOWED)
        }
      }
      AuthTokenType.ONLY_PAK -> {
        if (authenticationFacade.isPersonalAccessTokenAuth) {
          throw PermissionException(Message.PAT_ACCESS_NOT_ALLOWED)
        }
      }
      AuthTokenType.ANY -> {}
    }
  }

  /** Mirrors [RateLimitInterceptor.preHandle] */
  private fun applyRateLimit(spec: ToolEndpointSpec) {
    val policy = spec.rateLimitPolicy ?: return
    rateLimitService.checkPerUserRateLimit(
      "mcp.${spec.mcpOperation}",
      policy.limit,
      Duration.ofMillis(policy.refillDurationInMs),
    )
  }

  /** Mirrors [ReadOnlyModeInterceptor.preHandleInternal] */
  private fun checkReadOnlyMode(spec: ToolEndpointSpec) {
    if (!spec.isWriteOperation) return
    if (authenticationFacade.isReadOnly) {
      throw PermissionException(Message.OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE)
    }
  }

  /** Mirrors [OrganizationAuthorizationInterceptor.preHandleInternal], reuses [OrganizationRoleService.isUserOfRole] */
  private fun checkOrgRole(spec: ToolEndpointSpec) {
    val requiredRole = spec.requiredOrgRole ?: return
    val orgId = organizationHolder.organizationOrNull?.id ?: return
    val userId = authenticationFacade.authenticatedUser.id

    if (!organizationRoleService.isUserOfRole(userId, orgId, requiredRole)) {
      throw PermissionException()
    }
  }

  /** Mirrors [FeatureAuthorizationInterceptor.preHandleInternal], reuses [FeatureCheckService] */
  private fun checkFeatures(spec: ToolEndpointSpec) {
    val orgId = organizationHolder.organizationOrNull?.id ?: return

    if (spec.requiredFeatures != null) {
      featureCheckService.checkFeaturesEnabled(orgId, spec.requiredFeatures)
    }

    if (spec.requiredOneOfFeatures != null) {
      featureCheckService.checkOneOfFeaturesEnabled(orgId, spec.requiredOneOfFeatures)
    }
  }

  /** Mirrors [ActivityHandlerInterceptor.preHandle] */
  private fun setupActivity(spec: ToolEndpointSpec) {
    val activityType = spec.activityType ?: return
    activityHolder.activity = activityType
  }

  private fun emitPostHogEvent(spec: ToolEndpointSpec) {
    activityHolder.businessEventData["mcp"] = "true"
    activityHolder.businessEventData["mcp_operation"] = spec.mcpOperation
  }
}
