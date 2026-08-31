package io.tolgee.mcp

import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authentication.WriteOperation
import io.tolgee.security.authorization.IsGlobalRoute
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOneOfFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.security.ratelimit.RateLimited
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

data class RateLimitSpec(
  val limit: Int,
  val refillDurationInMs: Long,
)

data class ToolEndpointSpec(
  val mcpOperation: String,
  val requiredScopes: Array<Scope>?,
  val allowedTokenType: AuthTokenType,
  val isWriteOperation: Boolean,
  val useDefaultPermissions: Boolean = false,
  val isGlobalRoute: Boolean = false,
  val requiredOrgRole: OrganizationRoleType? = null,
  val requiredFeatures: Array<out Feature>? = null,
  val requiredOneOfFeatures: Array<out Feature>? = null,
  val activityType: ActivityType? = null,
  val rateLimitPolicy: RateLimitSpec? = null,
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
