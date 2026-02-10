package io.tolgee.mcp

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authentication.WriteOperation
import io.tolgee.security.authorization.IsGlobalRoute
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOneOfFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.ratelimit.RateLimited
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import java.time.Duration
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

data class RateLimitSpec(
  val limit: Int,
  val refillDurationInMs: Long,
)

data class ToolEndpointSpec(
  val mcpOperation: String,
  val requiredScopes: Array<Scope>?,
  val useDefaultPermissions: Boolean,
  val isGlobalRoute: Boolean,
  val requiredOrgRole: OrganizationRoleType?,
  val requiredFeatures: Array<out Feature>?,
  val requiredOneOfFeatures: Array<out Feature>?,
  val activityType: ActivityType?,
  val rateLimitPolicy: RateLimitSpec?,
  val allowedTokenType: AuthTokenType,
  val isWriteOperation: Boolean,
)

fun buildSpec(
  controllerMethod: KFunction<*>,
  mcpOperation: String,
): ToolEndpointSpec {
  val method =
    controllerMethod.javaMethod
      ?: error("MCP tool $mcpOperation: cannot resolve Java method from KFunction")
  return buildSpecFromMethod(method, mcpOperation)
}

fun buildSpec(
  controllerClass: Class<*>,
  methodName: String,
  mcpOperation: String,
  vararg paramTypes: Class<*>,
): ToolEndpointSpec {
  val method = controllerClass.getMethod(methodName, *paramTypes)
  return buildSpecFromMethod(method, mcpOperation)
}

private fun buildSpecFromMethod(
  method: java.lang.reflect.Method,
  mcpOperation: String,
): ToolEndpointSpec {
  val allowApiAccess = AnnotationUtils.getAnnotation(method, AllowApiAccess::class.java)
  require(allowApiAccess != null) {
    "MCP tool $mcpOperation references ${method.name} which lacks @AllowApiAccess"
  }

  require(AnnotationUtils.getAnnotation(method, RequiresSuperAuthentication::class.java) == null) {
    "MCP tool $mcpOperation references ${method.name} which requires super authentication (not supported in MCP)"
  }

  val rateLimited = AnnotationUtils.getAnnotation(method, RateLimited::class.java)

  return ToolEndpointSpec(
    mcpOperation = mcpOperation,
    requiredScopes = AnnotationUtils.getAnnotation(method, RequiresProjectPermissions::class.java)?.scopes,
    useDefaultPermissions = AnnotationUtils.getAnnotation(method, UseDefaultPermissions::class.java) != null,
    isGlobalRoute = AnnotationUtils.getAnnotation(method, IsGlobalRoute::class.java) != null,
    requiredOrgRole = AnnotationUtils.getAnnotation(method, RequiresOrganizationRole::class.java)?.role,
    requiredFeatures = AnnotationUtils.getAnnotation(method, RequiresFeatures::class.java)?.features,
    requiredOneOfFeatures = AnnotationUtils.getAnnotation(method, RequiresOneOfFeatures::class.java)?.features,
    allowedTokenType = allowApiAccess.tokenType,
    activityType = AnnotationUtils.getAnnotation(method, RequestActivity::class.java)?.activity,
    rateLimitPolicy = rateLimited?.let { RateLimitSpec(it.limit, it.refillDurationInMs) },
    isWriteOperation =
      when {
        AnnotationUtils.getAnnotation(method, WriteOperation::class.java) != null -> true
        AnnotationUtils.getAnnotation(method, ReadOnlyOperation::class.java) != null -> false
        else ->
          method.annotations.any {
            it is PostMapping || it is PutMapping || it is DeleteMapping || it is PatchMapping
          }
      },
  )
}

@Component
class McpSecurityContext(
  private val authenticationFacade: AuthenticationFacade,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val securityService: SecurityService,
  private val projectHolder: ProjectHolder,
  private val organizationHolder: OrganizationHolder,
  private val activityHolder: ActivityHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val rateLimitService: RateLimitService,
) {
  fun <T> executeAs(
    spec: ToolEndpointSpec,
    projectId: Long? = null,
    block: () -> T,
  ): T {
    checkTokenType(spec)
    applyRateLimit(spec)
    checkReadOnlyMode(spec)
    setupProjectContext(spec, projectId)
    checkOrgRole(spec, projectId)
    checkFeatures(spec)
    setupActivity(spec)
    emitPostHogEvent(spec)
    return block()
  }

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
      AuthTokenType.ANY -> {} // both are fine
    }
  }

  private fun applyRateLimit(spec: ToolEndpointSpec) {
    val policy = spec.rateLimitPolicy ?: return
    rateLimitService.checkPerUserRateLimit(
      "mcp.${spec.mcpOperation}",
      policy.limit,
      Duration.ofMillis(policy.refillDurationInMs),
    )
  }

  private fun checkReadOnlyMode(spec: ToolEndpointSpec) {
    if (!spec.isWriteOperation) return
    if (authenticationFacade.isReadOnly) {
      throw PermissionException(Message.OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE)
    }
  }

  private fun setupProjectContext(
    spec: ToolEndpointSpec,
    projectId: Long?,
  ) {
    if (spec.isGlobalRoute || projectId == null) return

    val project =
      projectService.findDto(projectId)
        ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)

    if (authenticationFacade.isProjectApiKeyAuth) {
      val pak = authenticationFacade.projectApiKey
      if (project.id != pak.projectId) {
        throw PermissionException(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
      }
    }

    projectHolder.project = project
    activityHolder.activityRevision.projectId = project.id
    organizationHolder.organization =
      organizationService.findDto(project.organizationOwnerId)
        ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)

    // Check permissions
    if (spec.requiredScopes != null) {
      val scopes = securityService.getCurrentPermittedScopes(project.id)
      val missing = spec.requiredScopes.toSet() - scopes
      if (missing.isNotEmpty()) {
        throw PermissionException(Message.OPERATION_NOT_PERMITTED, missing.map { it.value })
      }
    } else if (spec.useDefaultPermissions) {
      // Just check that the user has any access to the project
      securityService.checkAnyProjectPermission(project.id)
    }
  }

  private fun checkOrgRole(
    spec: ToolEndpointSpec,
    projectId: Long?,
  ) {
    val requiredRole = spec.requiredOrgRole ?: return
    val orgId = organizationHolder.organizationOrNull?.id ?: return
    val userId = authenticationFacade.authenticatedUser.id

    val userRole =
      organizationRoleService.findType(userId, orgId)
        ?: throw PermissionException()

    val hasRequiredRole =
      when (requiredRole) {
        OrganizationRoleType.OWNER -> userRole == OrganizationRoleType.OWNER
        OrganizationRoleType.MAINTAINER ->
          userRole == OrganizationRoleType.OWNER || userRole == OrganizationRoleType.MAINTAINER
        OrganizationRoleType.MEMBER -> true
      }

    if (!hasRequiredRole) {
      throw PermissionException()
    }
  }

  private fun checkFeatures(spec: ToolEndpointSpec) {
    val orgId = organizationHolder.organizationOrNull?.id ?: return

    if (spec.requiredFeatures != null) {
      val missing = spec.requiredFeatures.filter { !enabledFeaturesProvider.isFeatureEnabled(orgId, it) }
      if (missing.isNotEmpty()) {
        throw BadRequestException(Message.FEATURE_NOT_ENABLED, missing)
      }
    }

    if (spec.requiredOneOfFeatures != null) {
      val anyEnabled = spec.requiredOneOfFeatures.any { enabledFeaturesProvider.isFeatureEnabled(orgId, it) }
      if (!anyEnabled) {
        throw BadRequestException(Message.FEATURE_NOT_ENABLED, spec.requiredOneOfFeatures.toList())
      }
    }
  }

  private fun setupActivity(spec: ToolEndpointSpec) {
    val activityType = spec.activityType ?: return
    activityHolder.activity = activityType
  }

  private fun emitPostHogEvent(spec: ToolEndpointSpec) {
    activityHolder.businessEventData["mcp"] = "true"
    activityHolder.businessEventData["mcp_operation"] = spec.mcpOperation
  }
}
