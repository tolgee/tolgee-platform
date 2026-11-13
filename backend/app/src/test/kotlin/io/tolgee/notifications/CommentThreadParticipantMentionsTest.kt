package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NotificationsPocE2eTestData
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.NotificationType
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.JsonNode

class CommentThreadParticipantMentionsTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationsPocE2eTestData

  @BeforeEach
  fun setup() {
    testData = NotificationsPocE2eTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `earlier commenter who is not a member is offered first on a public project`() {
    loginAs(testData.user)
    val users = mentionable(testData.helpCenter.id, refundKeyId(), testData.helpCenterGerman.id)
    users.map { it.path("id").asLong() }.assert.containsExactly(testData.vera.id, testData.andreas.id)
    users[0]
      .path("inThread")
      .asBoolean()
      .assert
      .isTrue()
    users[1]
      .path("inThread")
      .asBoolean()
      .assert
      .isFalse()
    users[0].has("username").assert.isFalse()
  }

  @Test
  fun `non-member earlier commenter on a public project is notified and can open the thread`() {
    loginAs(testData.user)
    createComment(testData.helpCenter.id, refundKeyId(), testData.helpCenterGerman.id, "Agreed <@${testData.vera.id}>")
    mentionedUserIds(testData.helpCenter.id).assert.containsExactly(testData.vera.id)

    loginAs(testData.vera)
    performAuthGet(
      "/v2/projects/${testData.helpCenter.id}/translations/${testData.helpCenterRefundGerman.id}/comments",
    ).andIsOk
  }

  @Test
  fun `non-member who never commented in the thread is not notified on a public project`() {
    loginAs(testData.user)
    createComment(testData.helpCenter.id, refundKeyId(), testData.helpCenterGerman.id, "Hi <@${testData.anna.id}>")
    mentionedUserIds(testData.helpCenter.id).assert.isEmpty()
  }

  @Test
  fun `earlier commenter who lost access to a private project is neither offered nor notified`() {
    val payButton = keyId(testData.project.id, "checkout.pay_button")
    loginAs(testData.tom)
    createComment(testData.project.id, payButton, testData.french.id, "Is this the final wording?")
    loginAs(testData.user)
    mentionable(testData.project.id, payButton, testData.french.id)
      .map { it.path("id").asLong() }
      .assert
      .contains(testData.tom.id)

    performAuthPut("/v2/projects/${testData.project.id}/users/${testData.tom.id}/revoke-access", null).andIsOk

    mentionable(testData.project.id, payButton, testData.french.id)
      .map { it.path("id").asLong() }
      .assert
      .doesNotContain(testData.tom.id)
    createComment(testData.project.id, payButton, testData.french.id, "Yes <@${testData.tom.id}>")
    mentionedUserIds(testData.project.id).assert.isEmpty()
  }

  @Test
  fun `muted project sends no mention notification`() {
    loginAs(testData.vera)
    performAuthPut(
      "/v2/notification-settings/mentions/${testData.helpCenter.id}",
      mapOf("muted" to true),
    ).andIsOk
    val settings = performAuthGet("/v2/notification-settings/mentions").andIsOk.andGetContentAsString
    val helpCenter =
      objectMapper.readTree(settings).toList().single {
        it.path("projectId").asLong() ==
          testData.helpCenter.id
      }
    helpCenter
      .path("muted")
      .asBoolean()
      .assert
      .isTrue()

    loginAs(testData.user)
    createComment(testData.helpCenter.id, refundKeyId(), testData.helpCenterGerman.id, "Agreed <@${testData.vera.id}>")
    mentionedUserIds(testData.helpCenter.id).assert.isEmpty()
  }

  private fun loginAs(user: UserAccount) {
    userAccount = user
  }

  private fun refundKeyId() = keyId(testData.helpCenter.id, "faq.refund_policy")

  private fun keyId(
    projectId: Long,
    name: String,
  ): Long =
    jdbcTemplate.queryForObject(
      "select id from key where project_id = ? and name = ?",
      Long::class.java,
      projectId,
      name,
    )!!

  private fun mentionable(
    projectId: Long,
    keyId: Long,
    languageId: Long,
  ): List<JsonNode> {
    val body =
      performAuthGet("/v2/projects/$projectId/mentionable-users?keyId=$keyId&languageId=$languageId&size=20")
        .andIsOk.andGetContentAsString
    return objectMapper
      .readTree(body)
      .path("_embedded")
      .path("users")
      .toList()
  }

  private fun createComment(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    text: String,
  ) {
    performAuthPost(
      "/v2/projects/$projectId/translations/create-comment",
      mapOf("keyId" to keyId, "languageId" to languageId, "text" to text, "state" to "RESOLUTION_NOT_NEEDED"),
    ).andIsCreated
  }

  private fun mentionedUserIds(projectId: Long): List<Long> =
    jdbcTemplate
      .queryForList(
        "select user_id from notification where project_id = ? and type = ?",
        Long::class.java,
        projectId,
        NotificationType.COMMENT_MENTION.name,
      ).filterNotNull()
}
