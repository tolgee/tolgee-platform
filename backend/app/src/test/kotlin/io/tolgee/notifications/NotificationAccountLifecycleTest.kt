package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class NotificationAccountLifecycleTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val fanoutJobs by lazy { NotificationFanoutJobs(jdbcTemplate) }

  private var testData: NotificationRecipientsTestData? = null

  @AfterEach
  fun cleanup() {
    testData?.let { testDataService.cleanTestData(it.root) }
    userAccountService.findActive(SIGN_UP_EMAIL)?.let { userAccountService.delete(it) }
  }

  @Test
  fun `signing up with an organization does not notify the new user about the demo project`() {
    performPost(
      "/api/public/sign_up",
      SignUpDto(
        name = "New user",
        password = "very.strong.password",
        email = SIGN_UP_EMAIL,
        organizationName = "New organization",
      ),
    ).andIsOk

    val userId = userAccountService.findActive(SIGN_UP_EMAIL)!!.id
    jdbcTemplate
      .queryForList("select id from notification where user_id = ?", userId)
      .assert
      .isEmpty()
    jdbcTemplate
      .queryForList(
        "select j.id from tolgee_batch_job j join organization o on o.name = ? " +
          "join project p on p.organization_owner_id = o.id and p.id = j.project_id where j.type = 'NOTIFICATION_FANOUT'",
        "New organization",
      ).assert
      .isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `recipient and originator of notifications can delete their accounts`() {
    val data = NotificationRecipientsTestData()
    testDataService.saveTestData(data.root)
    testData = data
    userAccount = data.author
    projectSupplier = { data.project }
    performProjectAuthPost("keys", mapOf("name" to "new-key")).andIsCreated
    fanoutJobs.waitForAllSucceeded(data.project.id)
    jdbcTemplate
      .queryForList("select id from notification where user_id = ?", data.translatorFr.id)
      .assert
      .isNotEmpty()

    userAccount = data.translatorFr
    performAuthDelete("/v2/user").andIsOk
    userAccount = data.author
    performAuthDelete("/v2/user").andIsOk

    userAccountService.findActive(data.translatorFr.id).assert.isNull()
    userAccountService.findActive(data.author.id).assert.isNull()
  }

  companion object {
    private const val SIGN_UP_EMAIL = "notification-sign-up@example.com"
  }
}
