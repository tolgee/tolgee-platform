package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.dtos.request.importKeysResolvable.ResolvableTranslationResolution
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableItemRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableTranslationRequest
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.Project
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.activity.NotificationFanoutService
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate

class NotificationTranslationFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var fanoutService: NotificationFanoutService

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  private lateinit var otherProject: Project

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    otherProject =
      testData.root
        .addProject {
          name = "Other project"
          organizationOwner = testData.userAccountBuilder.defaultOrganizationBuilder.self
        }.build {
          val other = self
          addPermission {
            project = other
            user = testData.reviewerAll
            type = ProjectPermissionType.VIEW
          }
          val english = addEnglish()
          self.baseLanguage = english.self
          addKey("other-key")
        }.self
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun waitForFanout() {
    NotificationFanoutJobs(jdbcTemplate).waitForAllSucceeded(testData.project.id)
  }

  private fun notificationOf(
    userId: Long,
    type: NotificationType,
  ): Notification = notificationRepository.findAll().single { it.type == type && it.user.id == userId }

  private fun addTwoKeysAndNotify(): Notification {
    performProjectAuthPost("keys", mapOf("name" to "new-key-1")).andIsCreated
    performProjectAuthPost("keys", mapOf("name" to "new-key-2")).andIsCreated
    waitForFanout()
    return notificationOf(testData.reviewerAll.id, NotificationType.KEYS_ADDED)
  }

  private fun keyNamesFor(query: String): List<String> =
    objectMapper
      .readTree(performProjectAuthGet("translations?$query").andIsOk.andGetContentAsString)
      .path("_embedded")
      .path("keys")
      .toList()
      .map { it["keyName"].asText() }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filter by own notification returns only its keys`() {
    val notification = addTwoKeysAndNotify()
    jdbcTemplate
      .queryForObject(
        "select cardinality(activity_revision_ids) from notification where id = ?",
        Int::class.java,
        notification.id,
      ).assert
      .isEqualTo(2)

    userAccount = testData.reviewerAll
    keyNamesFor("filterNotificationId=${notification.id}").assert.containsExactlyInAnyOrder("new-key-1", "new-key-2")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notification of another user returns no keys`() {
    val notification = addTwoKeysAndNotify()

    userAccount = testData.reviewerFr
    performProjectAuthGet("translations?filterNotificationId=${notification.id}").andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `own notification opened in another project returns no keys`() {
    val notification = addTwoKeysAndNotify()

    userAccount = testData.reviewerAll
    projectSupplier = { otherProject }
    keyNamesFor("filterNotificationId=${notification.id}").assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notification filter combines with other filters`() {
    val notification = addTwoKeysAndNotify()

    userAccount = testData.reviewerAll
    keyNamesFor("filterNotificationId=${notification.id}&filterKeyName=new-key-2")
      .assert
      .containsExactly("new-key-2")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `select all honours the notification filter`() {
    val notification = addTwoKeysAndNotify()

    userAccount = testData.reviewerAll
    val body =
      performProjectAuthGet("translations/select-all?filterNotificationId=${notification.id}")
        .andIsOk.andGetContentAsString
    objectMapper
      .readTree(body)
      .path("ids")
      .toList()
      .map { it.asLong() }
      .assert
      .hasSize(2)
      .doesNotContain(testData.existingKey.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `keys added notification of an import shows only the added keys, not the changed ones`() {
    importNewKeyAndOverrideExistingFrench()
    waitForFanout()

    userAccount = testData.reviewerAll
    val keysAdded = notificationOf(testData.reviewerAll.id, NotificationType.KEYS_ADDED)
    keyNamesFor("filterNotificationId=${keysAdded.id}").assert.containsExactly("imported-key")
    val translated = notificationOf(testData.reviewerAll.id, NotificationType.STRINGS_TRANSLATED)
    keyNamesFor("filterNotificationId=${translated.id}").assert.containsExactly("existing-key")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `link and bell count agree for a revision mixing reviewed and only translated strings`() {
    performProjectAuthPost("keys", mapOf("name" to "second-key", "translations" to mapOf("en" to "Second")))
      .andIsCreated
    waitForFanout()
    deleteProjectNotifications()
    performProjectAuthPut("translations/${testData.existingGerman.id}/set-state/REVIEWED").andIsOk
    val reviewedRevisionId = latestRevisionId()
    performProjectAuthPut("translations", mapOf("key" to "second-key", "translations" to mapOf("de" to "Zweite")))
      .andIsOk
    waitForFanout()
    moveRevisionChanges(from = latestRevisionId(), to = reviewedRevisionId)
    deleteProjectNotifications()
    executeInNewTransaction { fanoutService.fanOut(reviewedRevisionId) }

    userAccount = testData.reviewerAll
    linkedKeyNamesMatchingBellCount(NotificationType.STRINGS_REVIEWED).assert.containsExactly("existing-key")
    linkedKeyNamesMatchingBellCount(NotificationType.STRINGS_TRANSLATED).assert.containsExactly("second-key")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `link and bell count agree for an import that adds keys and edits an existing source`() {
    import(
      "imported-key" to mapOf("en" to "Imported"),
      "existing-key" to mapOf("en" to "Hello changed"),
    )
    waitForFanout()

    userAccount = testData.reviewerAll
    linkedKeyNamesMatchingBellCount(NotificationType.KEYS_ADDED).assert.containsExactly("imported-key")
    linkedKeyNamesMatchingBellCount(NotificationType.SOURCE_CHANGED).assert.containsExactly("existing-key")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `language restricted recipient gets only keys changed in their languages`() {
    import(
      "existing-key" to mapOf("fr" to "Salut"),
      "german-key" to mapOf("de" to "Neu"),
    )
    waitForFanout()

    userAccount = testData.reviewerFr
    linkedKeyNamesMatchingBellCount(NotificationType.STRINGS_TRANSLATED).assert.containsExactly("existing-key")
    userAccount = testData.reviewerAll
    linkedKeyNames(NotificationType.STRINGS_TRANSLATED).assert.containsExactlyInAnyOrder("existing-key", "german-key")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `trash honours the notification filter`() {
    val notification = addTwoKeysAndNotify()
    val newKey1 = keyService.get(testData.project.id, "new-key-1", null)
    keyService.softDeleteMultiple(listOf(newKey1.id, testData.existingKey.id), testData.author)

    userAccount = testData.reviewerAll
    val body = performProjectAuthGet("keys/trash?filterNotificationId=${notification.id}").andIsOk.andGetContentAsString
    objectMapper
      .readTree(body)
      .path("_embedded")
      .path("keys")
      .toList()
      .map { it["name"].asText() }
      .assert
      .containsExactly("new-key-1")
  }

  @Test
  fun `querying with an unresolved notification filter fails`() {
    assertThrows<IllegalStateException> {
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages = emptySet(),
        pageable = PageRequest.of(0, 10),
        params = TranslationFilters().apply { filterNotificationId = 1L },
      )
    }
  }

  private fun latestRevisionId() =
    jdbcTemplate.queryForObject("select max(id) from activity_revision", Long::class.java)!!

  private fun deleteProjectNotifications() {
    jdbcTemplate.update("delete from notification where project_id = ?", testData.project.id)
  }

  private fun moveRevisionChanges(
    from: Long,
    to: Long,
  ) {
    jdbcTemplate.update(
      "update activity_modified_entity set activity_revision_id = ? where activity_revision_id = ?",
      to,
      from,
    )
  }

  private fun linkedKeyNames(type: NotificationType): List<String> {
    val notification = notificationOf(userAccount!!.id, type)
    return keyNamesFor("filterNotificationId=${notification.id}")
  }

  private fun linkedKeyNamesMatchingBellCount(type: NotificationType): List<String> {
    val notification = notificationOf(userAccount!!.id, type)
    val keyNames = keyNamesFor("filterNotificationId=${notification.id}")
    keyNames.assert.hasSize(bellCount(notification.id))
    return keyNames
  }

  private fun bellCount(notificationId: Long): Int =
    objectMapper
      .readTree(performAuthGet("/v2/notification?size=100").andIsOk.andGetContentAsString)
      .path("_embedded")
      .path("notificationModelList")
      .single { it["id"].asLong() == notificationId }["entityCount"]
      .asInt()

  private fun import(vararg keys: Pair<String, Map<String, String>>) {
    performProjectAuthPost(
      "single-step-import-resolvable",
      SingleStepImportResolvableRequest(
        keys =
          keys.map { (name, translations) ->
            SingleStepImportResolvableItemRequest(
              name = name,
              translations =
                translations.mapValues { (_, text) ->
                  SingleStepImportResolvableTranslationRequest(
                    text = text,
                    resolution = ResolvableTranslationResolution.OVERRIDE,
                  )
                },
            )
          },
      ),
    ).andIsOk
  }

  private fun importNewKeyAndOverrideExistingFrench() {
    performProjectAuthPost(
      "single-step-import-resolvable",
      SingleStepImportResolvableRequest(
        keys =
          listOf(
            SingleStepImportResolvableItemRequest(
              name = "imported-key",
              translations =
                mapOf(
                  "en" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "Imported",
                      resolution = ResolvableTranslationResolution.EXPECT_NO_CONFLICT,
                    ),
                ),
            ),
            SingleStepImportResolvableItemRequest(
              name = "existing-key",
              translations =
                mapOf(
                  "fr" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "Salut",
                      resolution = ResolvableTranslationResolution.OVERRIDE,
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk
  }
}
