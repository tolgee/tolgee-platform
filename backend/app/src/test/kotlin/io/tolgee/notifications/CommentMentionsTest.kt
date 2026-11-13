package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode

class CommentMentionsTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData(frenchOnlyViewerCount = 3)
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun token(id: Long) = "<@$id>"

  private fun mentionedUserIds() =
    notificationRepository
      .findAll()
      .filter { it.type == NotificationType.COMMENT_MENTION }
      .map { it.user.id }

  private fun createComment(text: String) =
    performProjectAuthPost(
      "translations/create-comment",
      mapOf(
        "keyId" to testData.existingKey.id,
        "languageId" to testData.french.id,
        "text" to text,
        "state" to "RESOLUTION_NOT_NEEDED",
      ),
    ).andIsCreated

  private fun createdCommentId(text: String): Long {
    val response = createComment(text).andGetContentAsString
    val id = Regex("\"comment\":\\{\"id\":(\\d+)").find(response)!!.groupValues[1]
    return id.toLong()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `only members who can view the language are mentioned and notified`() {
    val text =
      listOf(testData.reviewerAll.id, testData.viewerDe.id, testData.outsider.id, testData.author.id, 999999L)
        .joinToString(" ") { token(it) }
    createComment(text).andAssertThatJson {
      node("comment.text").isEqualTo(text)
      node("comment.mentions").isArray.hasSize(2)
      node("comment.mentions[0].id").isEqualTo(testData.reviewerAll.id)
      node("comment.mentions[0].name").isEqualTo("reviewerAll")
      node("comment.mentions[0].username").isAbsent()
      node("comment.mentions[1].id").isEqualTo(testData.author.id)
    }
    mentionedUserIds().assert.containsExactly(testData.reviewerAll.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing a comment notifies only newly mentioned users`() {
    val commentId = createdCommentId("Hi ${token(testData.reviewerAll.id)}")
    performProjectAuthPut(
      "translations/${testData.existingFrench.id}/comments/$commentId",
      mapOf(
        "text" to "Hi ${token(testData.reviewerAll.id)} and ${token(testData.translatorFr.id)}",
        "state" to "RESOLUTION_NOT_NEEDED",
      ),
    ).andIsOk.andAssertThatJson {
      node("mentions").isArray.hasSize(2)
    }
    mentionedUserIds().assert.containsExactlyInAnyOrder(testData.reviewerAll.id, testData.translatorFr.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deleting a comment deletes its mention notifications`() {
    val commentId = createdCommentId("Hi ${token(testData.reviewerAll.id)}")
    mentionedUserIds().assert.hasSize(1)
    performProjectAuthDelete("translations/${testData.existingFrench.id}/comments/$commentId").andIsOk
    mentionedUserIds().assert.isEmpty()
  }

  private fun mentionable(query: String): List<JsonNode> {
    val body = performProjectAuthGet("mentionable-users?$query").andIsOk.andGetContentAsString
    return objectMapper
      .readTree(body)
      .path("_embedded")
      .path("users")
      .toList()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mentionable users can view the language and expose no email`() {
    val french = mentionable("languageId=${testData.french.id}&size=100")
    french
      .map { it["id"].asLong() }
      .assert
      .contains(testData.reviewerAll.id, testData.translatorFr.id, testData.reviewerFr.id)
      .doesNotContain(testData.viewerDe.id, testData.outsider.id, testData.author.id)
    french.forEach { it.has("username").assert.isFalse() }
    mentionable("languageId=${testData.german.id}")
      .map { it["id"].asLong() }
      .assert
      .contains(testData.viewerDe.id, testData.reviewerAll.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mentionable users page is filled past members who cannot view the language`() {
    val firstPage = mentionable("languageId=${testData.german.id}&size=2")
    firstPage.assert.hasSize(2)
    firstPage
      .map { it["id"].asLong() }
      .assert
      .doesNotContainAnyElementsOf(testData.frenchOnlyViewers.map { it.id })

    val body =
      performProjectAuthGet("mentionable-users?languageId=${testData.german.id}&size=100")
        .andIsOk.andGetContentAsString
    val all = objectMapper.readTree(body)
    all
      .path("page")
      .path("totalElements")
      .asLong()
      .assert
      .isEqualTo(
        all
          .path("_embedded")
          .path("users")
          .size()
          .toLong(),
      )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mentionable users can be searched by name`() {
    mentionable("languageId=${testData.french.id}&search=REVIEWER")
      .map { it["name"].asText() }
      .assert
      .containsExactly("reviewerAll", "reviewerFr")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `editing a comment to remove a mention forgets the unseen mention notification`() {
    val commentId = createdCommentId("Hi ${token(testData.reviewerAll.id)} and ${token(testData.translatorFr.id)}")
    performProjectAuthPut(
      "translations/${testData.existingFrench.id}/comments/$commentId",
      mapOf("text" to "Hi ${token(testData.translatorFr.id)}", "state" to "RESOLUTION_NOT_NEEDED"),
    ).andIsOk
    mentionedUserIds().assert.containsExactly(testData.translatorFr.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mention notification carries the linked comment`() {
    val commentId = createdCommentId("Please check ${token(testData.reviewerAll.id)}")
    userAccount = testData.reviewerAll
    performAuthGet("/v2/notification").andIsOk.andAssertThatJson {
      node("_embedded.notificationModelList[0].type").isEqualTo("COMMENT_MENTION")
      node("_embedded.notificationModelList[0].linkedComment.id").isEqualTo(commentId)
      node("_embedded.notificationModelList[0].linkedComment.keyName").isEqualTo("existing-key")
      node("_embedded.notificationModelList[0].linkedComment.languageTag").isEqualTo("fr")
      node("_embedded.notificationModelList[0].linkedComment.mentions[0].id").isEqualTo(testData.reviewerAll.id)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `single notification is returned only to its user`() {
    createComment("Please check ${token(testData.reviewerAll.id)}")
    val notificationId = notificationRepository.findAll().single { it.type == NotificationType.COMMENT_MENTION }.id
    userAccount = testData.reviewerAll
    performAuthGet("/v2/notification/$notificationId").andIsOk.andAssertThatJson {
      node("id").isEqualTo(notificationId)
      node("linkedComment.keyName").isEqualTo("existing-key")
    }
    userAccount = testData.translatorFr
    performAuthGet("/v2/notification/$notificationId").andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mentionable users need the comment permission`() {
    userAccount = testData.viewerDe
    performProjectAuthGet("mentionable-users?languageId=${testData.german.id}").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `mentionable users need to view the language`() {
    userAccount = testData.translatorFr
    performProjectAuthGet("mentionable-users?languageId=${testData.german.id}").andIsForbidden
  }
}
