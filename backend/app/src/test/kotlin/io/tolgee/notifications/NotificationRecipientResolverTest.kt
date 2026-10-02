package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.activity.ClassifiedRevision
import io.tolgee.service.notification.activity.NotificationRecipientResolver
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NotificationRecipientResolverTest : AbstractSpringTest() {
  @Autowired
  private lateinit var resolver: NotificationRecipientResolver

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
  }

  private fun revision(items: Map<NotificationType, Map<Long?, List<Long>>>) =
    ClassifiedRevision(
      revisionId = 1,
      projectId = testData.project.id,
      authorId = testData.author.id,
      baseLanguageId = testData.englishLanguage.id,
      items = items,
    )

  private fun resolve(items: Map<NotificationType, Map<Long?, List<Long>>>) =
    executeInNewTransaction(platformTransactionManager) { resolver.resolve(revision(items)) }

  @Test
  fun `keys added go to translators of a non-base language, not to the author`() {
    val result = resolve(mapOf(NotificationType.KEYS_ADDED to mapOf(null to listOf(10L, 11L))))
    result.map { it.userId }.assert.containsExactlyInAnyOrder(
      testData.translatorFr.id,
      testData.reviewerFr.id,
      testData.reviewerAll.id,
    )
    result
      .first()
      .entityIds.assert
      .containsExactly(10L, 11L)
  }

  @Test
  fun `translated goes to reviewers of that language only, with only their entities`() {
    val fr = testData.french.id
    val de = testData.german.id
    val result =
      resolve(mapOf(NotificationType.STRINGS_TRANSLATED to mapOf(fr to listOf(1L), de to listOf(2L, 3L))))
    val byUser = result.associateBy { it.userId }
    byUser.keys.assert.containsExactlyInAnyOrder(testData.reviewerFr.id, testData.reviewerAll.id)
    byUser[testData.reviewerFr.id]!!.entityIds.assert.containsExactly(1L)
    byUser[testData.reviewerAll.id]!!.entityIds.assert.containsExactlyInAnyOrder(1L, 2L, 3L)
  }

  @Test
  fun `bulk changed goes to viewers of the language, including org members`() {
    val de = testData.german.id
    val result = resolve(mapOf(NotificationType.BULK_CHANGED to mapOf(de to listOf(de))))
    result.map { it.userId }.assert.containsExactlyInAnyOrder(
      testData.reviewerAll.id,
      testData.viewerDe.id,
      testData.orgMember.id,
    )
  }

  @Test
  fun `outsider never gets anything`() {
    val result =
      resolve(
        mapOf(
          NotificationType.KEYS_ADDED to mapOf(null to listOf(1L)),
          NotificationType.STRINGS_REVIEWED to mapOf(testData.german.id to listOf(2L)),
        ),
      )
    result.map { it.userId }.assert.doesNotContain(testData.outsider.id, testData.author.id)
  }

  @Test
  fun `entity ids are capped at 100 per recipient`() {
    val fr = testData.french.id
    val de = testData.german.id
    val result =
      resolve(
        mapOf(NotificationType.STRINGS_TRANSLATED to mapOf(fr to (1L..100L).toList(), de to (101L..200L).toList())),
      )
    result
      .single { it.userId == testData.reviewerAll.id }
      .entityIds.assert
      .hasSize(100)
  }
}
