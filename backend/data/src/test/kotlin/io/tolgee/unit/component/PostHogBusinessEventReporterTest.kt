package io.tolgee.unit.component

import com.posthog.server.PostHog
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.component.reporting.PostHogBusinessEventReporter
import io.tolgee.component.reporting.PostHogGroupIdentifier.Companion.GROUP_TYPE
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class PostHogBusinessEventReporterTest {
  private val postHog: PostHog = mock()

  private val reporter =
    PostHogBusinessEventReporter(
      postHog = postHog,
      projectService = mock(),
      organizationService = mock(),
      userAccountService = mock(),
      entityManager = mock(),
      postHogGroupIdentifier = null,
    )

  private val forgedAttribution =
    mapOf(
      "organizationId" to 999L,
      "organizationName" to "Acme",
      "glossaryId" to 999L,
      "${'$'}groups" to mapOf(GROUP_TYPE to 999L),
    )

  @Test
  fun `client data cannot overwrite server attribution`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "ORDINARY_EVENT",
        organizationId = 7L,
        organizationName = "Org",
        glossaryId = 5L,
        userAccountId = 1L,
        anonymousUserId = "anon",
        utmData = forgedAttribution + ("utm_source" to "kept"),
        data = forgedAttribution + ("keep" to "me"),
      ),
    )

    val properties = assertPostHogEventReported(postHog, "ORDINARY_EVENT")
    properties["organizationId"].assert.isEqualTo(7L)
    properties["organizationName"].assert.isEqualTo("Org")
    properties["glossaryId"].assert.isEqualTo(5L)
    (properties["${'$'}groups"] as Map<*, *>)[GROUP_TYPE].assert.isEqualTo(7L)
    properties["utm_source"].assert.isEqualTo("kept")
    properties["keep"].assert.isEqualTo("me")
  }

  @Test
  fun `client data cannot forge attribution the server did not resolve`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "ORDINARY_EVENT",
        anonymousUserId = "anon",
        data = forgedAttribution,
      ),
    )

    val properties = assertPostHogEventReported(postHog, "ORDINARY_EVENT")
    properties.containsKey("organizationId").assert.isFalse
    properties.containsKey("organizationName").assert.isFalse
    properties.containsKey("glossaryId").assert.isFalse
    (properties["${'$'}groups"] as Map<*, *>)[GROUP_TYPE].assert.isNull()
  }

  @Test
  fun `reserved client keys are dropped`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "ORDINARY_EVENT",
        anonymousUserId = "anon",
        data =
          mapOf(
            "${'$'}set_once" to mapOf("email" to "attacker@example.com"),
            "distinct_id" to "victim",
            "token" to "other-project",
            "keep" to "me",
          ),
      ),
    )

    val properties = assertPostHogEventReported(postHog, "ORDINARY_EVENT")
    properties.containsKey("${'$'}set_once").assert.isFalse
    properties.containsKey("distinct_id").assert.isFalse
    properties.containsKey("token").assert.isFalse
    properties["keep"].assert.isEqualTo("me")
  }

  @Test
  fun `reserved event name is refused`() {
    reporter.captureAsync(
      OnBusinessEventToCaptureEvent(
        eventName = "${'$'}create_alias",
        anonymousUserId = "anon",
      ),
    )

    verifyNoInteractions(postHog)
  }
}
