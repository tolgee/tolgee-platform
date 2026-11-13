package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.FollowedLanguagesState
import io.tolgee.service.notification.NotificationProjectPreferenceService
import io.tolgee.service.notification.activity.ClassifiedRevision
import io.tolgee.service.notification.activity.NotificationRecipientResolver
import io.tolgee.service.security.ProjectMemberPermissionProvider
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.Date

class NotificationProjectPreferenceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var resolver: NotificationRecipientResolver

  @Autowired
  private lateinit var projectPreferenceService: NotificationProjectPreferenceService

  @Autowired
  private lateinit var projectMemberPermissionProvider: ProjectMemberPermissionProvider

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    setForcedDate(Date())
    testData = NotificationRecipientsTestData()
    testData.projectBuilder.addTranslationReview(testData.reviewerAll, testData.existingGerman)
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
    testDataService.cleanTestData(testData.root)
  }

  private fun resolve(items: Map<NotificationType, Map<Long?, List<Long>>>) =
    executeInNewTransaction(platformTransactionManager) {
      resolver.resolve(
        ClassifiedRevision(
          revisionId = 1,
          projectId = testData.project.id,
          authorId = testData.author.id,
          baseLanguageId = testData.englishLanguage.id,
          items = items,
        ),
      )
    }

  private fun reviewerAllGets(
    type: NotificationType,
    byLanguage: Map<Long?, List<Long>>,
  ) = resolve(mapOf(type to byLanguage)).singleOrNull { it.userId == testData.reviewerAll.id }

  private fun frenchChange() =
    reviewerAllGets(NotificationType.STRINGS_TRANSLATED, mapOf(testData.french.id to listOf(1L)))

  private fun germanChange() =
    reviewerAllGets(NotificationType.STRINGS_TRANSLATED, mapOf(testData.german.id to listOf(2L)))

  private fun followedByReviewerAll() =
    executeInNewTransaction(platformTransactionManager) {
      projectPreferenceService.followedLanguages(
        testData.project.id,
        projectMemberPermissionProvider.findMembers(testData.project.id, listOf(testData.reviewerAll.id)),
        listOf(testData.french.id, testData.german.id),
      )[testData.reviewerAll.id]
    }

  private fun deleteGerman() =
    executeInNewTransaction(platformTransactionManager) {
      val german = languageService.getEntity(testData.german.id)
      permissionService.removeLanguageFromPermissions(german)
      languageService.hardDeleteLanguage(german)
    }

  private fun choose(languageIds: List<Long>?) =
    projectPreferenceService.set(testData.reviewerAll.id, testData.project.id, languageIds)

  @Test
  fun `default follows the languages worked on recently`() {
    val followed = followedByReviewerAll()
    followed?.state.assert.isEqualTo(FollowedLanguagesState.DEFAULT)
    followed?.languageIds.assert.containsExactly(testData.german.id)
    frenchChange().assert.isNull()
    germanChange()?.entityIds.assert.containsExactly(2L)
  }

  @Test
  fun `default falls back to editable languages without recent work`() {
    moveCurrentDate(Duration.ofDays(91))
    followedByReviewerAll()
      ?.languageIds
      .assert
      .containsExactlyInAnyOrder(testData.french.id, testData.german.id)
    frenchChange()?.entityIds.assert.containsExactly(1L)
  }

  @Test
  fun `own choice replaces the default and is kept`() {
    choose(listOf(testData.french.id))
    moveCurrentDate(Duration.ofDays(2))
    val followed = followedByReviewerAll()
    followed?.state.assert.isEqualTo(FollowedLanguagesState.CHOSEN)
    followed?.languageIds.assert.containsExactly(testData.french.id)
    frenchChange()?.entityIds.assert.containsExactly(1L)
    germanChange().assert.isNull()
  }

  @Test
  fun `mixed change keeps only the entities of followed languages`() {
    reviewerAllGets(
      NotificationType.AUTOMATICALLY_TRANSLATED,
      mapOf(testData.french.id to listOf(1L), testData.german.id to listOf(2L)),
    )?.entityIds
      .assert
      .containsExactly(2L)
  }

  @Test
  fun `null goes back to the default`() {
    choose(listOf(testData.french.id))
    choose(null)
    val followed = followedByReviewerAll()
    followed?.state.assert.isEqualTo(FollowedLanguagesState.DEFAULT)
    followed?.languageIds.assert.containsExactly(testData.german.id)
  }

  @Test
  fun `empty choice mutes the project`() {
    choose(emptyList())
    followedByReviewerAll()?.state.assert.isEqualTo(FollowedLanguagesState.MUTED)
    reviewerAllGets(NotificationType.KEYS_ADDED, mapOf(null to listOf(10L))).assert.isNull()
    germanChange().assert.isNull()
  }

  @Test
  fun `new keys reach a user whose followed language is editable`() {
    reviewerAllGets(NotificationType.KEYS_ADDED, mapOf(null to listOf(10L)))?.entityIds.assert.containsExactly(10L)
  }

  @Test
  fun `new keys skip a user who follows no editable language`() {
    projectPreferenceService.set(testData.reviewerFr.id, testData.project.id, listOf(testData.german.id))
    resolve(mapOf(NotificationType.KEYS_ADDED to mapOf(null to listOf(10L))))
      .map { it.userId }
      .assert
      .doesNotContain(testData.reviewerFr.id)
      .contains(testData.translatorFr.id, testData.reviewerAll.id)
  }

  @Test
  fun `viewer without followable languages is not narrowed`() {
    resolve(mapOf(NotificationType.STRINGS_REVIEWED to mapOf(testData.german.id to listOf(2L))))
      .map { it.userId }
      .assert
      .contains(testData.viewerDe.id)
  }

  @Test
  fun `deleting the only chosen language goes back to the default`() {
    choose(listOf(testData.german.id))
    deleteGerman()
    followedByReviewerAll()?.state.assert.isEqualTo(FollowedLanguagesState.DEFAULT)
  }

  @Test
  fun `deleting a chosen language keeps the rest of the choice`() {
    choose(listOf(testData.german.id, testData.french.id))
    deleteGerman()
    val followed = followedByReviewerAll()
    followed?.state.assert.isEqualTo(FollowedLanguagesState.CHOSEN)
    followed?.languageIds.assert.containsExactly(testData.french.id)
  }
}
