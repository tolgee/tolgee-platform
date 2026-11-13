package io.tolgee.notifications

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.model.translation.Translation
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationMentionMuteService
import io.tolgee.service.notification.NotificationProjectPreferenceService
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.notification.activity.GroupedNotificationWrite
import io.tolgee.service.notification.activity.GroupedNotificationWriter
import io.tolgee.service.notification.digest.NotificationEmailJob
import io.tolgee.service.notification.digest.NotificationEmailQueries
import io.tolgee.service.translation.TranslationCommentService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mail.MailSendException
import java.time.Duration
import java.util.Date

class NotificationEmailJobTest : AbstractSpringTest() {
  @Autowired
  private lateinit var job: NotificationEmailJob

  @Autowired
  private lateinit var emailQueries: NotificationEmailQueries

  @Autowired
  private lateinit var writer: GroupedNotificationWriter

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  private lateinit var digestStateService: NotificationDigestStateService

  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var notificationService: NotificationService

  @Autowired
  private lateinit var commentService: TranslationCommentService

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var projectPreferenceService: NotificationProjectPreferenceService

  @Autowired
  private lateinit var mentionMuteService: NotificationMentionMuteService

  private lateinit var testData: NotificationRecipientsTestData

  private var originalFrontEndUrl: String? = null

  @BeforeEach
  fun setup() {
    emailTestUtil.initMocks()
    tolgeeProperties.smtp.host = "localhost"
    originalFrontEndUrl = tolgeeProperties.frontEndUrl
    tolgeeProperties.frontEndUrl = "https://app.example.com"
    testData = NotificationRecipientsTestData()
    testData.author.name = "Anna Novak"
    testDataService.saveTestData(testData.root)
    setForcedDate(Date())
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
    tolgeeProperties.smtp.host = null
    tolgeeProperties.frontEndUrl = originalFrontEndUrl
  }

  private fun recordWorkChange(
    type: NotificationType = NotificationType.KEYS_ADDED,
    ids: List<Long> = listOf(1L),
  ) = executeInNewTransaction(platformTransactionManager) {
    writer.write(
      GroupedNotificationWrite(
        userId = testData.reviewerAll.id,
        projectId = testData.project.id,
        originatingUserId = testData.author.id,
        type = type,
        entityIds = ids,
        emailEnabled = true,
      ),
    )
  }

  private fun mention(
    text: String = "Can you check this, <@${testData.reviewerAll.id}>?",
    authorId: Long = testData.author.id,
  ) = executeInNewTransaction(platformTransactionManager) {
    val translation = entityManager.find(Translation::class.java, testData.existingFrench.id)
    val author = entityManager.find(UserAccount::class.java, authorId)
    commentService.create(TranslationCommentDto(text = text), translation, author)
  }

  private fun lastSubject() =
    emailTestUtil.messageArgumentCaptor.allValues
      .last()
      .subject

  private fun mentionRows() =
    notificationRepository.findAll().filter {
      it.type == NotificationType.COMMENT_MENTION && it.user.id == testData.reviewerAll.id
    }

  private fun advanceMinutes(value: Long) = moveCurrentDate(Duration.ofMinutes(value))

  @Test
  fun `work email waits for 15 quiet minutes`() {
    recordWorkChange()
    advanceMinutes(10)
    recordWorkChange(ids = listOf(2L))
    advanceMinutes(10)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("Notification project")
      .contains("new key")
      .contains("/translations?notification=")
  }

  @Test
  fun `work email is sent at most 60 minutes after the item was created`() {
    recordWorkChange()
    (1..5).forEach {
      advanceMinutes(10)
      recordWorkChange(ids = listOf(it + 1L))
      job.sendDueEmails()
    }
    emailTestUtil.verifyTimesEmailSent(0)
    advanceMinutes(11)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
  }

  @Test
  fun `only one work email per 4 hours`() {
    recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)

    recordWorkChange(NotificationType.AUTOMATICALLY_TRANSLATED, listOf(testData.existingFrench.id))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)

    moveCurrentDate(Duration.ofHours(4))
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
  }

  @Test
  fun `work email also carries pending work items that are not due yet`() {
    recordWorkChange()
    advanceMinutes(10)
    recordWorkChange(NotificationType.AUTOMATICALLY_TRANSLATED, listOf(testData.existingFrench.id))
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("new key")
      .contains("Auto-translated")

    advanceMinutes(30)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
  }

  @Test
  fun `daily frequency waits 24 hours between work emails`() {
    digestStateService.setFrequency(testData.reviewerAll.id, NotificationDigestFrequency.DAILY)
    recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    recordWorkChange(NotificationType.SOURCE_CHANGED)
    settingsService.saveForType(
      testData.reviewerAll,
      NotificationType.SOURCE_CHANGED,
      NotificationChannel.EMAIL,
      true,
    )
    moveCurrentDate(Duration.ofHours(5))
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    moveCurrentDate(Duration.ofHours(20))
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
  }

  @Test
  fun `mention email is sent 5 minutes after the mention`() {
    mention()
    mentionRows().assert.hasSize(1)
    advanceMinutes(4)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    advanceMinutes(2)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("Anna Novak mentioned you")
      .contains("<b>@reviewerAll</b>")
      .doesNotContain("&lt;@")
      .contains("/translations/single?id=${testData.existingKey.id}")
    emailTestUtil.messageArgumentCaptor.firstValue.subject.assert
      .isEqualTo("Anna Novak mentioned you")
  }

  @Test
  fun `mention ignores the 4 hour work limit and is combined with due work`() {
    recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)

    recordWorkChange(ids = listOf(2L, 3L))
    mention()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
    emailTestUtil.messageContents[1]
      .assert
      .contains("mentioned you")
      .doesNotContain("new key")

    moveCurrentDate(Duration.ofHours(4))
    recordWorkChange(ids = listOf(4L))
    mention("Again <@${testData.reviewerAll.id}>")
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(3)
    emailTestUtil.messageContents[2]
      .assert
      .contains("mentioned you")
      .contains("new key")
    emailTestUtil.messageArgumentCaptor.allValues[2]
      .subject.assert
      .startsWith("Anna Novak mentioned you, and ")
  }

  @Test
  fun `seen notifications are not emailed`() {
    val work = recordWorkChange()
    mention()
    notificationService.markNotificationsAsSeen(
      listOf(work.id) + mentionRows().map { it.id },
      testData.reviewerAll.id,
    )
    advanceMinutes(61)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
  }

  @Test
  fun `mention from a project muted after the mention is not emailed and leaves the bell`() {
    mention()
    mentionRows().assert.hasSize(1)
    mentionMuteService.set(testData.reviewerAll.id, testData.project.id, muted = true)
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    mentionRows().assert.isEmpty()
  }

  @Test
  fun `frequency off stops work emails but not mentions`() {
    digestStateService.setFrequency(testData.reviewerAll.id, NotificationDigestFrequency.OFF)
    recordWorkChange()
    mention()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert.doesNotContain("new key")
  }

  @Test
  fun `type disabled for email after writing is skipped and cleared`() {
    val n = recordWorkChange()
    settingsService.saveForType(testData.reviewerAll, NotificationType.KEYS_ADDED, NotificationChannel.EMAIL, false)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.messageContents.assert.isEmpty()
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isFalse()
  }

  @Test
  fun `sent rows store the emailed count`() {
    val n = recordWorkChange(ids = listOf(1L, 2L, 3L))
    advanceMinutes(16)
    job.sendDueEmails()
    notificationRepository
      .findById(n.id)
      .get()
      .emailedCount.assert
      .isEqualTo(3)
  }

  @Test
  fun `message id is the same after a crash before commit and changes with new content`() {
    val n = recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    val first = messageId(0)
    first.assert.startsWith("<digest-${testData.reviewerAll.id}-")

    jdbcTemplate.update("update notification set email_pending = true where id = ?", n.id)
    jdbcTemplate.update(
      "update notification_digest_state set last_digest_sent_at = null where user_id = ?",
      testData.reviewerAll.id,
    )
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
    messageId(1).assert.isEqualTo(first)

    moveCurrentDate(Duration.ofHours(5))
    recordWorkChange(ids = listOf(5L))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(3)
    messageId(2).assert.isNotEqualTo(first)
  }

  @Test
  fun `content written during sending keeps the row pending`() {
    val n = recordWorkChange()
    advanceMinutes(16)
    whenever(emailTestUtil.javaMailSender.send(any<MimeMessage>())).thenAnswer {
      moveCurrentDate(Duration.ofSeconds(1))
      recordWorkChange(ids = listOf(7L))
    }
    job.sendDueEmails()
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
  }

  @Test
  fun `lost-access delete keeps a row that got new content after it was loaded`() {
    val n = recordWorkChange()
    val loaded = emailQueries.findPendingWork(testData.reviewerAll.id).single().pending
    moveCurrentDate(Duration.ofSeconds(1))
    recordWorkChange(ids = listOf(7L))
    emailQueries.deleteIfUnchanged(listOf(loaded))
    notificationRepository
      .findById(n.id)
      .orElse(null)
      .assert.isNotNull
  }

  @Test
  fun `lost-access delete removes a row that did not change since it was loaded`() {
    val n = recordWorkChange()
    val loaded = emailQueries.findPendingWork(testData.reviewerAll.id).single().pending
    emailQueries.deleteIfUnchanged(listOf(loaded))
    notificationRepository
      .findById(n.id)
      .orElse(null)
      .assert
      .isNull()
  }

  @Test
  fun `does nothing when smtp is not configured`() {
    tolgeeProperties.smtp.host = null
    val n = recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    verify(emailTestUtil.javaMailSender, never()).createMimeMessage()
    emailTestUtil.verifyTimesEmailSent(0)
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()
    lastDigestSentAt().assert.isNull()
  }

  @Test
  fun `failed send stays pending and is retried in the next run`() {
    val n = recordWorkChange()
    advanceMinutes(16)
    whenever(emailTestUtil.javaMailSender.send(any<MimeMessage>())).thenThrow(MailSendException("no connection"))
    job.sendDueEmails()
    lastDigestSentAt().assert.isNull()
    notificationRepository
      .findById(n.id)
      .get()
      .emailPending.assert
      .isTrue()

    whenever(emailTestUtil.javaMailSender.send(any<MimeMessage>())).thenAnswer { }
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
    lastDigestSentAt().assert.isNotNull()
  }

  @Test
  fun `notifications of a deleted project are not sent`() {
    recordWorkChange()
    jdbcTemplate.update("update project set deleted_at = now() where id = ?", testData.project.id)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
  }

  @Test
  fun `message id uses the sender domain when the frontend url has no host`() {
    tolgeeProperties.frontEndUrl = "tolgee-host"
    tolgeeProperties.smtp.from = "Tolgee <no-reply@mail.example.org>"
    recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    messageId(0).assert.endsWith("@mail.example.org>")
  }

  @Test
  fun `reused work row waits for a new quiet window after its first email`() {
    recordWorkChange()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)

    moveCurrentDate(Duration.ofHours(5))
    recordWorkChange(ids = listOf(2L))
    advanceMinutes(1)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    advanceMinutes(15)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
  }

  @Test
  fun `due mention alone does not send work that is not due yet`() {
    recordWorkChange()
    mention()
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.messageContents[0].assert.doesNotContain("new key")

    advanceMinutes(10)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(2)
    emailTestUtil.messageContents[1].assert.contains("new key")
  }

  @Test
  fun `mention with email off is shown in app but not emailed`() {
    settingsService.save(testData.reviewerAll, NotificationTypeGroup.COMMENTS, NotificationChannel.EMAIL, false)
    mention()
    mentionRows()
      .single()
      .emailPending.assert
      .isFalse()
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
  }

  @Test
  fun `mention with in-app off and email on is emailed but never shown in app`() {
    settingsService.save(testData.reviewerAll, NotificationTypeGroup.COMMENTS, NotificationChannel.IN_APP, false)
    mention()
    notificationService
      .getNotifications(testData.reviewerAll.id)
      .content.assert
      .isEmpty()
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert.contains("Anna Novak mentioned you")
    mentionRows().assert.isEmpty()
  }

  @Test
  fun `mention is not queued for email when smtp is not configured`() {
    tolgeeProperties.smtp.host = null
    mention()
    mentionRows()
      .single()
      .emailPending.assert
      .isFalse()

    settingsService.save(testData.reviewerAll, NotificationTypeGroup.COMMENTS, NotificationChannel.IN_APP, false)
    jdbcTemplate.update("delete from notification")
    mention()
    mentionRows().assert.isEmpty()
  }

  @Test
  fun `delayed mention email is dropped when the recipient lost access to the language`() {
    mention()
    restrictReviewerAllTo(testData.german)
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    mentionRows().assert.isEmpty()
  }

  @Test
  fun `delayed mention email is dropped when the recipient left the project`() {
    mention()
    permissionService.revoke(testData.reviewerAll.id, testData.project.id)
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    mentionRows().assert.isEmpty()
  }

  @Test
  fun `work email is dropped when the recipient lost access to its languages`() {
    recordWorkChange(type = NotificationType.AUTOMATICALLY_TRANSLATED, ids = listOf(testData.existingFrench.id))
    restrictReviewerAllTo(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.AUTOMATICALLY_TRANSLATED).assert.isEmpty()
  }

  @Test
  fun `work email is sent when the recipient can still act on one of its languages`() {
    recordWorkChange(type = NotificationType.AUTOMATICALLY_TRANSLATED, ids = listOf(testData.existingFrench.id))
    restrictReviewerAllTo(testData.french)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
  }

  @Test
  fun `reviewer downgraded to view-only gets no automatically translated email`() {
    recordWorkChange(type = NotificationType.AUTOMATICALLY_TRANSLATED, ids = listOf(testData.existingFrench.id))
    downgradeReviewerAllToViewOnly()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.AUTOMATICALLY_TRANSLATED).assert.isEmpty()
  }

  @Test
  fun `translator downgraded to view-only gets no new keys email`() {
    recordWorkChange()
    downgradeReviewerAllToViewOnly()
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.KEYS_ADDED).assert.isEmpty()
  }

  @Test
  fun `work email is dropped when the recipient left the project`() {
    recordWorkChange()
    permissionService.revoke(testData.reviewerAll.id, testData.project.id)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.KEYS_ADDED).assert.isEmpty()
  }

  @Test
  fun `work email lists only the languages and strings the recipient can still act on`() {
    val n =
      recordWorkChange(
        type = NotificationType.AUTOMATICALLY_TRANSLATED,
        ids = listOf(testData.existingFrench.id, testData.branchFrench.id, testData.existingGerman.id),
      )
    restrictReviewerAllTo(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("Auto-translated: German")
      .doesNotContain("French")
    notificationRepository
      .findById(n.id)
      .get()
      .emailedCount.assert
      .isEqualTo(1)
  }

  @Test
  fun `only the work row the recipient lost access to is dropped`() {
    recordWorkChange()
    recordWorkChange(type = NotificationType.AUTOMATICALLY_TRANSLATED, ids = listOf(testData.existingFrench.id))
    restrictReviewerAllTo(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("new key")
      .doesNotContain("Auto-translated")
    workRows(NotificationType.AUTOMATICALLY_TRANSLATED).assert.isEmpty()
    workRows(NotificationType.KEYS_ADDED).assert.hasSize(1)
  }

  @Test
  fun `work email lists only the languages the recipient chose`() {
    recordWorkChange(
      type = NotificationType.AUTOMATICALLY_TRANSLATED,
      ids = listOf(testData.existingFrench.id, testData.existingGerman.id),
    )
    chooseForReviewerAll(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
    emailTestUtil.singleEmailContent.assert
      .contains("Auto-translated: German")
      .doesNotContain("French")
  }

  @Test
  fun `work email is dropped when the recipient chose none of its languages`() {
    recordWorkChange(type = NotificationType.AUTOMATICALLY_TRANSLATED, ids = listOf(testData.existingFrench.id))
    chooseForReviewerAll(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.AUTOMATICALLY_TRANSLATED).assert.isEmpty()
  }

  @Test
  fun `new keys email is still sent when a chosen language is actionable`() {
    recordWorkChange()
    chooseForReviewerAll(testData.german)
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(1)
  }

  @Test
  fun `work email is dropped when the recipient muted the project`() {
    recordWorkChange()
    projectPreferenceService.set(testData.reviewerAll.id, testData.project.id, emptyList())
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.verifyTimesEmailSent(0)
    workRows(NotificationType.KEYS_ADDED).assert.isEmpty()
  }

  private fun chooseForReviewerAll(language: Language) =
    projectPreferenceService.set(testData.reviewerAll.id, testData.project.id, listOf(language.id))

  private fun restrictReviewerAllTo(language: Language) =
    executeInNewTransaction(platformTransactionManager) {
      val permission = permissionService.getOrCreateDirectPermission(testData.project.id, testData.reviewerAll.id)
      val languages = mutableSetOf(entityManager.getReference(Language::class.java, language.id))
      permission.type = ProjectPermissionType.REVIEW
      permission.viewLanguages = languages
      permission.translateLanguages = languages.toMutableSet()
      permission.stateChangeLanguages = languages.toMutableSet()
      permissionService.save(permission)
    }

  private fun downgradeReviewerAllToViewOnly() =
    executeInNewTransaction(platformTransactionManager) {
      val permission = permissionService.getOrCreateDirectPermission(testData.project.id, testData.reviewerAll.id)
      permission.type = ProjectPermissionType.VIEW
      permissionService.save(permission)
    }

  private fun workRows(type: NotificationType) =
    notificationRepository.findAll().filter { it.type == type && it.user.id == testData.reviewerAll.id }

  @Test
  fun `mention email escapes user written text and names`() {
    jdbcTemplate.update("update user_account set name = '<i>Anna</i>' where id = ?", testData.author.id)
    mention("<b>bold</b> & <@${testData.reviewerAll.id}> <@999999>")
    advanceMinutes(6)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert
      .contains("&lt;i&gt;Anna&lt;/i&gt; mentioned you")
      .contains("&lt;b&gt;bold&lt;/b&gt; &amp; <b>@reviewerAll</b> &lt;@999999&gt;")
      .doesNotContain("<b>bold</b>")
      .doesNotContain("<i>Anna</i>")
  }

  @Test
  fun `work email lists non-default branches`() {
    recordWorkChange(ids = listOf(testData.branchKey.id))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert.contains("feature-login")
  }

  @Test
  fun `one new key shows its name and base text`() {
    recordWorkChange(ids = listOf(testData.existingKey.id))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert.contains("New key existing-key “Hello”")
  }

  @Test
  fun `two new keys show their names and base texts`() {
    recordWorkChange(ids = listOf(testData.existingKey.id, testData.branchKey.id))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert.contains("2 new keys: existing-key “Hello” and branch-key “Login”")
  }

  @Test
  fun `three or more new keys show two examples and the rest as a count`() {
    recordWorkChange(ids = listOf(testData.existingKey.id, testData.branchKey.id, 1L))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert
      .contains("3 new keys in Notification project, e.g. existing-key, branch-key and 1 more")
  }

  @Test
  fun `capped new keys show two examples without a remaining count`() {
    recordWorkChange(ids = listOf(testData.existingKey.id, testData.branchKey.id) + (1L..98L))
    advanceMinutes(16)
    job.sendDueEmails()
    emailTestUtil.singleEmailContent.assert
      .contains("100+ new keys in Notification project, e.g. existing-key, branch-key and more")
  }

  @Test
  fun `subject counts new keys`() {
    recordWorkChange(ids = listOf(1L, 2L, 3L))
    advanceMinutes(16)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("3 new keys are waiting")
  }

  @Test
  fun `subject marks a capped key count`() {
    recordWorkChange(ids = (1L..100L).toList())
    advanceMinutes(16)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("100+ new keys are waiting")
  }

  @Test
  fun `subject of work without new keys`() {
    recordWorkChange(NotificationType.AUTOMATICALLY_TRANSLATED, listOf(testData.existingFrench.id))
    advanceMinutes(16)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("There are updates in your projects")
  }

  @Test
  fun `subject combines a mention with new keys`() {
    recordWorkChange()
    advanceMinutes(10)
    mention()
    advanceMinutes(6)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("Anna Novak mentioned you, and 1 new key is waiting")
  }

  @Test
  fun `subject uses singular for one new key`() {
    recordWorkChange(ids = listOf(1L))
    advanceMinutes(16)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("1 new key is waiting")
  }

  @Test
  fun `subject counts mentions from several authors`() {
    mention()
    mention(authorId = testData.reviewerFr.id)
    advanceMinutes(6)
    job.sendDueEmails()
    lastSubject().assert.isEqualTo("You were mentioned 2 times")
  }

  private fun lastDigestSentAt() =
    jdbcTemplate
      .query(
        "select last_digest_sent_at from notification_digest_state where user_id = ?",
        { rs, _ -> rs.getTimestamp(1) },
        testData.reviewerAll.id,
      ).firstOrNull()

  private fun messageId(index: Int) = emailTestUtil.messageArgumentCaptor.allValues[index].getHeader("Message-ID")[0]
}
