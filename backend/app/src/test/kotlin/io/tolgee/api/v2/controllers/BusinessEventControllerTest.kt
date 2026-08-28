package io.tolgee.api.v2.controllers

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BusinessEventTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.model.UserAccount
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

class BusinessEventControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BusinessEventTestData

  @Autowired
  lateinit var postHog: PostHog

  private val foreignOrganizationBuilder get() = testData.foreignOrganizationBuilder
  private val memberOrganizationBuilder get() = testData.memberOrganizationBuilder
  private val outsider get() = testData.outsider
  private val admin get() = testData.admin
  private val supporter get() = testData.supporter
  private val softDeletedProject get() = testData.softDeletedProject

  @BeforeEach
  fun setup() {
    testData = BusinessEventTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it accepts header`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "TEST_EVENT",
        "organizationId" to testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
        "projectId" to testData.projectBuilder.self.id,
        "data" to mapOf("test" to "test"),
      ),
      bearerHeadersFor(userAccount!!),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "TEST_EVENT")
    params["organizationId"].assert.isNotNull
    params["organizationName"].assert.isEqualTo("test_username")
    params["test"].assert.isEqualTo("test")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops an organization claim the reporter cannot back`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "OUTSIDER_EVENT",
        "organizationId" to testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "OUTSIDER_EVENT")
    params["organizationId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it derives attribution from the project the event was fired in, dropping a foreign claim`() {
    val foreignOrg = foreignOrganizationBuilder.self
    executeInNewTransaction {
      projectService.get(testData.projectBuilder.self.id).public = true
    }

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "PUBLIC_PROJECT_EVENT",
        "organizationId" to foreignOrg.id,
        "projectId" to testData.projectBuilder.self.id,
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "PUBLIC_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isEqualTo(testData.projectBuilder.self.id)
    params["projectId"].assert.isEqualTo(testData.projectBuilder.self.id)
    params["organizationId"].assert.isEqualTo(
      testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it keeps a claim the reporter is a member of`() {
    val owner = testData.userAccountBuilder.self
    val ownOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "MEMBER_EVENT",
        "organizationId" to ownOrg.id,
      ),
      bearerHeadersFor(owner),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "MEMBER_EVENT")
    params["organizationId"].assert.isEqualTo(ownOrg.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it keeps a private project claim from someone with access to that project`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "OWN_PRIVATE_PROJECT_EVENT",
        "projectId" to testData.projectBuilder.self.id,
      ),
      bearerHeadersFor(testData.user),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "OWN_PRIVATE_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isEqualTo(testData.projectBuilder.self.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a project claim the reporter cannot back but keeps the event`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "PRIVATE_PROJECT_EVENT",
        "projectId" to testData.projectBuilder.self.id,
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "PRIVATE_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isNull()
    params["organizationId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a claim for a project that no longer exists but keeps the event`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "DELETED_PROJECT_EVENT",
        "projectId" to Long.MAX_VALUE,
      ),
      bearerHeadersFor(testData.user),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "DELETED_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a bare organization claim backed only by that organization having public projects`() {
    val publicOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self
    executeInNewTransaction {
      projectService.get(testData.projectBuilder.self.id).public = true
    }

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "UNADOPTED_EVENT",
        "organizationId" to publicOrg.id,
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "UNADOPTED_EVENT")
    params["organizationId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it marks a kept claim from a member as a member claim`() {
    val ownOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "MEMBER_MARKED_EVENT",
        "organizationId" to ownOrg.id,
      ),
      bearerHeadersFor(testData.userAccountBuilder.self),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "MEMBER_MARKED_EVENT")
    params["organizationMember"].assert.isEqualTo(true)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it answers membership for an organization derived from the reported project`() {
    executeInNewTransaction {
      projectService.get(testData.projectBuilder.self.id).public = true
    }

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "DERIVED_ORG_EVENT",
        "projectId" to testData.projectBuilder.self.id,
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "DERIVED_ORG_EVENT")
    params.postHogGroupedProjectId().assert.isEqualTo(testData.projectBuilder.self.id)
    params["organizationMember"].assert.isEqualTo(false)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it does not count staff browsing a customer organization as its members`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_MEMBERSHIP_EVENT",
        "organizationId" to foreignOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(admin),
    ).andIsOk
    assertPostHogEventReported(postHog, "ADMIN_MEMBERSHIP_EVENT")["organizationMember"]
      .assert
      .isEqualTo(false)

    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "SUPPORTER_MEMBERSHIP_EVENT",
        "organizationId" to foreignOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(supporter),
    ).andIsOk
    assertPostHogEventReported(postHog, "SUPPORTER_MEMBERSHIP_EVENT")["organizationMember"]
      .assert
      .isEqualTo(false)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a project claim from an anonymous reporter but keeps the event`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ANONYMOUS_PROJECT_EVENT",
        "projectId" to testData.projectBuilder.self.id,
      ),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ANONYMOUS_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops an admin claim for an organization that does not exist`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_EVENT",
        "organizationId" to Long.MAX_VALUE,
      ),
      bearerHeadersFor(admin),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ADMIN_EVENT")
    params["organizationId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it keeps an admin claim for an organization the admin does not belong to`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_VIEW_EVENT",
        "organizationId" to foreignOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(admin),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ADMIN_VIEW_EVENT")
    params["organizationId"].assert.isEqualTo(foreignOrganizationBuilder.self.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it does not let the free-form data map overwrite the authorized attribution`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "DATA_MAP_EVENT",
        "organizationId" to testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
        "data" to
          mapOf(
            "organizationId" to 999999,
            "organizationName" to "Acme",
            "projectId" to 999999,
          ),
      ),
      bearerHeadersFor(outsider),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "DATA_MAP_EVENT")
    params["organizationId"].assert.isNull()
    params["organizationName"].assert.isNull()
    params["projectId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it keeps a supporter claim for an organization the supporter does not belong to`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "SUPPORTER_VIEW_EVENT",
        "organizationId" to foreignOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(supporter),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "SUPPORTER_VIEW_EVENT")
    params["organizationId"].assert.isEqualTo(foreignOrganizationBuilder.self.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops an organization claim that does not own the reported project`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "CROSS_ATTRIBUTION_EVENT",
        "projectId" to testData.projectBuilder.self.id,
        "organizationId" to memberOrganizationBuilder.self.id,
      ),
      bearerHeadersFor(testData.user),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "CROSS_ATTRIBUTION_EVENT")
    params.postHogGroupedProjectId().assert.isEqualTo(testData.projectBuilder.self.id)
    params["organizationId"].assert.isEqualTo(
      testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it still identifies the person it reports for`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf("eventName" to "IDENTIFIED_EVENT"),
      bearerHeadersFor(testData.user),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "IDENTIFIED_EVENT")
    val setEntry = params["\u0024set"] as Map<*, *>
    setEntry["email"].assert.isEqualTo(testData.user.username)
    setEntry["name"].assert.isEqualTo(testData.user.name)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it keeps a staff claim for a project the staff user holds no permission in`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_PROJECT_EVENT",
        "projectId" to testData.projectBuilder.self.id,
      ),
      bearerHeadersFor(admin),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ADMIN_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isEqualTo(testData.projectBuilder.self.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a staff claim for a project that does not exist`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_DELETED_PROJECT_EVENT",
        "projectId" to Long.MAX_VALUE,
      ),
      bearerHeadersFor(admin),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ADMIN_DELETED_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops a staff claim for a soft-deleted project, along with the organization it would attribute`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ADMIN_SOFT_DELETED_PROJECT_EVENT",
        "projectId" to softDeletedProject.id,
      ),
      bearerHeadersFor(admin),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ADMIN_SOFT_DELETED_PROJECT_EVENT")
    params.postHogGroupedProjectId().assert.isNull()
    params["organizationId"].assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it refuses to let an anonymous report write PostHog person state`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "PERSON_STATE_EVENT",
        "anonymousUserId" to "anonymous-attacker",
        "data" to
          mapOf(
            "\u0024set" to mapOf("email" to "attacker@example.com"),
            "\u0024set_once" to mapOf("name" to "Attacker"),
            "\u0024unset" to listOf("email"),
            "harmless" to "kept",
          ),
      ),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "PERSON_STATE_EVENT")
    params["\u0024set"].assert.isNull()
    params["\u0024set_once"].assert.isNull()
    params["\u0024unset"].assert.isNull()
    params["harmless"].assert.isEqualTo("kept")
    params["\u0024anon_distinct_id"].assert.isEqualTo("anonymous-attacker")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it drops an anonymous organization claim`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "ANONYMOUS_EVENT",
        "organizationId" to testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
      ),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "ANONYMOUS_EVENT")
    params["organizationId"].assert.isNull()
  }

  private fun Map<*, *>.postHogGroupedProjectId(): Any? = (this["\u0024groups"] as? Map<*, *>)?.get("project")

  private fun bearerHeadersFor(user: UserAccount): HttpHeaders =
    HttpHeaders().also {
      it["Authorization"] = AuthorizedRequestFactory.getBearerTokenString(generateJwtToken(user.id))
    }
}
