package io.tolgee.unit

import com.posthog.server.PostHog
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.component.reporting.PostHogBusinessEventReporter
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.Organization
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class PostHogBusinessEventReporterTest {
  private val postHog = Mockito.mock(PostHog::class.java)
  private val organizationService = Mockito.mock(OrganizationService::class.java)
  private val userAccountService = Mockito.mock(UserAccountService::class.java)
  private val entityManager = Mockito.mock(EntityManager::class.java)
  private val organizationRoleService = Mockito.mock(OrganizationRoleService::class.java)

  private val reporter =
    PostHogBusinessEventReporter(
      postHog = postHog,
      projectService = Mockito.mock(ProjectService::class.java),
      organizationService = organizationService,
      userAccountService = userAccountService,
      entityManager = entityManager,
      postHogGroupIdentifier = null,
      organizationRoleService = organizationRoleService,
    )

  private fun capturedProperties(): Map<*, *> =
    Mockito
      .mockingDetails(postHog)
      .invocations
      .first { it.method.name == "capture" }
      .getArgument(2)

  private fun captureInvocationCount() =
    Mockito
      .mockingDetails(postHog)
      .invocations
      .count { it.method.name == "capture" }

  @Test
  fun `it refuses a reserved event name outright`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "${'$'}create_alias",
        anonymousUserId = "anonymous-attacker",
      ),
    )

    captureInvocationCount().assert.isEqualTo(0)
  }

  @Test
  fun `it omits organizationMember when the publisher named no user`() {
    val ownerId = 42L
    val organization = Mockito.mock(Organization::class.java)
    Mockito.`when`(organization.name).thenReturn("Owner Org")
    Mockito.`when`(organizationService.get(7L)).thenReturn(organization)

    @Suppress("UNCHECKED_CAST")
    val query = Mockito.mock(TypedQuery::class.java) as TypedQuery<Long>
    Mockito
      .`when`(entityManager.createQuery(Mockito.anyString(), Mockito.eq(Long::class.java)))
      .thenReturn(query)
    Mockito.`when`(query.setParameter(Mockito.anyString(), Mockito.any())).thenReturn(query)
    Mockito.`when`(query.resultList).thenReturn(listOf(ownerId))
    // The owner really resolves: without this the substitution never materialises and the test
    // would pass whichever event the actor is read from.
    Mockito.`when`(userAccountService.findDto(ownerId)).thenReturn(
      UserAccountDto(
        name = "Owner",
        username = "owner@example.com",
        domain = null,
        role = null,
        id = ownerId,
        needsSuperJwt = false,
        avatarHash = null,
        deleted = false,
        tokensValidNotBefore = null,
        emailVerified = true,
        thirdPartyAuth = null,
        ssoRefreshToken = null,
        ssoSessionExpiry = null,
      ),
    )

    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "SERVER_INITIATED_EVENT",
        organizationId = 7L,
      ),
    )

    // fillOtherData substitutes the organization OWNER as the attribution account, so reading the
    // actor from the filled copy would report that owner's membership as the actor's.
    capturedProperties().containsKey("organizationMember").assert.isEqualTo(false)
    Mockito
      .verify(organizationRoleService, Mockito.never())
      .hasAnyOrganizationRole(Mockito.anyLong(), Mockito.anyLong())
  }

  @Test
  fun `it reports an ordinary event name`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "ORDINARY_EVENT",
        anonymousUserId = "anonymous-visitor",
      ),
    )

    captureInvocationCount().assert.isEqualTo(1)
  }
}
