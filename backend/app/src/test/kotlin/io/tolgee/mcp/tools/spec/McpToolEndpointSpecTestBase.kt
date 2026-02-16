package io.tolgee.mcp.tools.spec

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.RateLimitSpec
import io.tolgee.mcp.ToolEndpointSpec
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectContextService
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.OrganizationFeatureGuard
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.service.organization.OrganizationRoleService
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

abstract class McpToolEndpointSpecTestBase {
  protected lateinit var authenticationFacade: AuthenticationFacade
  protected lateinit var organizationRoleService: OrganizationRoleService
  protected lateinit var organizationHolder: OrganizationHolder
  protected lateinit var activityHolder: ActivityHolder
  protected lateinit var organizationFeatureGuard: OrganizationFeatureGuard
  protected lateinit var rateLimitService: RateLimitService
  protected lateinit var projectContextService: ProjectContextService

  protected lateinit var sut: McpRequestContext

  protected val businessEventData = mutableMapOf<String, String?>()

  @BeforeEach
  fun setUp() {
    businessEventData.clear()
    authenticationFacade = mock()
    organizationRoleService = mock()
    organizationHolder = mock()
    activityHolder = mock()
    organizationFeatureGuard = mock()
    rateLimitService = mock()
    projectContextService = mock()

    whenever(activityHolder.businessEventData).thenReturn(businessEventData)

    sut =
      McpRequestContext(
        authenticationFacade,
        organizationRoleService,
        organizationHolder,
        activityHolder,
        organizationFeatureGuard,
        rateLimitService,
        projectContextService,
      )
  }

  protected fun spec(
    mcpOperation: String = "test_op",
    allowedTokenType: AuthTokenType = AuthTokenType.ANY,
    requiredScopes: Array<Scope>? = null,
    useDefaultPermissions: Boolean = false,
    isGlobalRoute: Boolean = true,
    requiredOrgRole: OrganizationRoleType? = null,
    requiredFeatures: Array<out Feature>? = null,
    requiredOneOfFeatures: Array<out Feature>? = null,
    activityType: ActivityType? = null,
    rateLimitPolicy: RateLimitSpec? = null,
    isWriteOperation: Boolean = false,
  ) = ToolEndpointSpec(
    mcpOperation = mcpOperation,
    requiredScopes = requiredScopes,
    allowedTokenType = allowedTokenType,
    isWriteOperation = isWriteOperation,
    useDefaultPermissions = useDefaultPermissions,
    isGlobalRoute = isGlobalRoute,
    requiredOrgRole = requiredOrgRole,
    requiredFeatures = requiredFeatures,
    requiredOneOfFeatures = requiredOneOfFeatures,
    activityType = activityType,
    rateLimitPolicy = rateLimitPolicy,
  )
}
