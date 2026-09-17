package io.tolgee.api.v2.controllers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.posthog.server.PostHog
import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class BusinessEventControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: PublicProjectsControllerTestData

  @Autowired
  lateinit var postHog: PostHog

  @BeforeEach
  fun setup() {
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `it accepts member's own organization and private project`() {
    reportAs(
      testData.user,
      mapOf(
        "eventName" to "TEST_EVENT",
        "organizationId" to testData.privateProject.organizationOwner.id,
        "projectId" to testData.privateProject.id,
        "data" to mapOf("test" to "test"),
      ),
    )

    val params = assertPostHogEventReported(postHog, "TEST_EVENT")
    params["organizationId"].assert.isNotNull
    params["organizationName"].assert.isEqualTo(testData.privateProject.organizationOwner.name)
    params["test"].assert.isEqualTo("test")
  }

  @Test
  fun `it accepts organization claim from non-member when the organization owns a public project`() {
    reportAs(
      testData.nonMember,
      mapOf(
        "eventName" to "PUBLIC_ORG_EVENT",
        "organizationId" to testData.publicProject.organizationOwner.id,
      ),
    )

    val params = assertPostHogEventReported(postHog, "PUBLIC_ORG_EVENT")
    params["organizationName"].assert.isEqualTo(testData.publicProject.organizationOwner.name)
  }

  @Test
  fun `it accepts public project claim from non-member`() {
    reportAs(
      testData.nonMember,
      mapOf(
        "eventName" to "PUBLIC_PROJECT_EVENT",
        "organizationId" to testData.publicProject.organizationOwner.id,
        "projectId" to testData.publicProject.id,
      ),
    )

    val params = assertPostHogEventReported(postHog, "PUBLIC_PROJECT_EVENT")
    (params["\$groups"] as Map<*, *>)["project"].assert.isEqualTo(testData.publicProject.id)
  }

  @Test
  fun `it does not log an error for non-member claim on organization without public projects`() {
    val logged =
      capturingControllerLog {
        reportAs(
          testData.nonMember,
          mapOf(
            "eventName" to "PRIVATE_ORG_EVENT",
            "organizationId" to testData.noPublicOrg.id,
          ),
        )
      }

    logged.filter { it.level == Level.ERROR }.assert.isEmpty()
  }

  @Test
  fun `it does not log an error for deleted project claim`() {
    val logged =
      capturingControllerLog {
        reportAs(
          testData.user,
          mapOf(
            "eventName" to "DELETED_PROJECT_EVENT",
            "projectId" to testData.deletedPublicProject.id,
          ),
        )
      }

    logged.filter { it.level == Level.ERROR }.assert.isEmpty()
  }

  private fun reportAs(
    user: UserAccount,
    body: Map<String, Any>,
  ) {
    userAccount = user
    performAuthPost("/v2/public/business-events/report", body).andIsOk
  }

  private fun capturingControllerLog(block: () -> Unit): List<ILoggingEvent> {
    val logger = LoggerFactory.getLogger(BusinessEventController::class.java) as Logger
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    logger.addAppender(appender)
    try {
      block()
    } finally {
      logger.detachAppender(appender)
    }
    return appender.list
  }
}
