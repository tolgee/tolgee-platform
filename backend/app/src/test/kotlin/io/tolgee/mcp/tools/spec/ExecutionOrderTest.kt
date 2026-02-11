package io.tolgee.mcp.tools.spec

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.mcp.RateLimitSpec
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AuthTokenType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExecutionOrderTest : McpSecurityTestBase() {
  @Test
  fun `interceptors execute in correct order`() {
    val inOrder =
      Mockito.inOrder(
        rateLimitService,
        authenticationFacade,
        projectContextService,
        organizationRoleService,
        organizationHolder,
        featureCheckService,
        activityHolder,
      )

    val orgDto = mock<OrganizationDto>()
    whenever(orgDto.id).thenReturn(1L)
    whenever(organizationHolder.organizationOrNull).thenReturn(orgDto)

    val userDto = mock<UserAccountDto>()
    whenever(userDto.id).thenReturn(1L)
    whenever(authenticationFacade.authenticatedUser).thenReturn(userDto)
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isProjectApiKeyAuth).thenReturn(false)
    whenever(authenticationFacade.isPersonalAccessTokenAuth).thenReturn(false)
    whenever(authenticationFacade.isReadOnly).thenReturn(false)
    whenever(organizationRoleService.isUserOfRole(any(), any(), any())).thenReturn(true)

    val policy = RateLimitSpec(limit = 10, refillDurationInMs = 1000)
    val features = arrayOf(Feature.GRANULAR_PERMISSIONS)

    val testSpec =
      spec(
        mcpOperation = "ordered_op",
        rateLimitPolicy = policy,
        allowedTokenType = AuthTokenType.ANY,
        isGlobalRoute = false,
        requiredScopes = arrayOf(Scope.KEYS_VIEW),
        useDefaultPermissions = false,
        requiredOrgRole = OrganizationRoleType.MEMBER,
        isWriteOperation = true,
        requiredFeatures = features,
        activityType = ActivityType.CREATE_KEY,
      )

    sut.executeAs(testSpec, projectId = 5L) {}

    // 1. Rate limit
    inOrder.verify(rateLimitService).checkPerUserRateLimit(any(), any(), any())
    // 2. Token type check
    inOrder.verify(authenticationFacade).isApiAuthentication
    // 3. Project context setup
    inOrder.verify(projectContextService).setup(any<Long>(), any(), any(), any())
    // 4. Org role check
    inOrder.verify(organizationHolder).organizationOrNull
    // 5. Read-only mode
    inOrder.verify(authenticationFacade).isReadOnly
    // 6. Feature check
    inOrder.verify(featureCheckService).checkFeaturesEnabled(any(), any())
    // 7. Activity setup
    inOrder.verify(activityHolder).activity = ActivityType.CREATE_KEY
    // 8. PostHog event (accesses businessEventData twice — once per map put)
    inOrder.verify(activityHolder, atLeast(1)).businessEventData
  }
}
