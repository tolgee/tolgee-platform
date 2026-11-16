package io.tolgee.service.notification.digest

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.language.LanguageService
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationEligibility
import io.tolgee.service.notification.NotificationMentionMuteService
import io.tolgee.service.notification.NotificationProjectPreferenceService
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.security.ProjectMemberPermissionProvider
import io.tolgee.service.security.ProjectMemberWithPermission
import io.tolgee.service.translation.CommentMentionService
import io.tolgee.service.translation.CommentMentionSource
import io.tolgee.service.translation.MentionableUser
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.net.URI
import java.sql.Timestamp
import java.time.Duration

@Component
class NotificationEmailJob(
  private val queries: NotificationEmailQueries,
  private val composer: NotificationDigestComposer,
  private val emailSender: TolgeeEmailSender,
  private val digestStateService: NotificationDigestStateService,
  private val settingsService: NotificationSettingsService,
  private val commentMentionService: CommentMentionService,
  private val projectMemberPermissionProvider: ProjectMemberPermissionProvider,
  private val languageService: LanguageService,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
  private val schedulingManager: SchedulingManager,
  private val projectPreferenceService: NotificationProjectPreferenceService,
  private val mentionMuteService: NotificationMentionMuteService,
) : Logging {
  private val props get() = tolgeeProperties.notifications

  @EventListener(ApplicationReadyEvent::class)
  fun schedule() {
    if (!props.schedulingEnabled) return
    schedulingManager.scheduleWithFixedDelay(::sendDueEmails, Duration.ofMillis(props.emailJobIntervalMs))
  }

  fun sendDueEmails() {
    if (!tolgeeProperties.smtp.hasHostAndSender()) return
    lockingProvider.withLockingIfFree(LOCK_NAME, LEASE_TIME) {
      val deadline = System.currentTimeMillis() + LEASE_TIME.toMillis() * 4 / 5
      for (recipient in queries.findDueRecipients(now(), soonCutoff(), dailyCutoff())) {
        if (System.currentTimeMillis() >= deadline) break
        try {
          emailDueNotificationsAndUpdatePending(recipient)
        } catch (e: Exception) {
          logger.error("Sending notification email to user ${recipient.userId} failed, it stays pending", e)
        }
      }
    }
  }

  private fun emailDueNotificationsAndUpdatePending(recipient: EmailRecipient) {
    val userId = recipient.userId
    val dueMentions = queries.findDueMentions(userId, now())
    val mentionedByComment = mentionedMembersByComment(dueMentions)
    val mentions = keepMentionsAndDeleteUnreachable(userId, dueMentions, mentionedByComment)
    val work = narrowWorkAndDeleteUnreachable(userId, findWorkIfWindowElapsed(userId))
    val enabledTypes = emailEnabledTypes(userId, mentions.map { it.pending.type } + work.map { it.pending.type })
    val (sendableMentions, disabledMentions) = mentions.partition { it.pending.type in enabledTypes }
    val (enabledWork, disabledWork) = work.partition { it.pending.type in enabledTypes }
    queries.clearPendingAndDropEmailOnly(disabledMentions.map { it.pending } + disabledWork.map { it.pending })

    val sendableWork = workIfAnyDue(enabledWork)
    if (sendableMentions.isEmpty() && sendableWork.isEmpty()) return

    val sent = sendableMentions.map { it.pending } + sendableWork.map { it.pending }
    val email =
      composer.compose(
        rows = sendableWork.map { it.row },
        entityCap = props.entityCap,
        mentions = toMentionRows(sendableMentions, mentionedByComment),
      )
    emailSender.sendEmail(
      EmailParams(
        to = recipient.email,
        subject = email.subject,
        text = email.html,
        messageId = messageId(userId, sent),
      ),
    )
    markSent(userId, sent, sendableWork)
  }

  private fun mentionedMembersByComment(mentions: List<DueMention>) =
    commentMentionService.findMentionedMembersBySource(
      mentions.map { CommentMentionSource(it.commentId, it.projectId, it.keyId, it.languageId, it.text) },
    )

  private fun keepMentionsAndDeleteUnreachable(
    userId: Long,
    dueMentions: List<DueMention>,
    mentionedByComment: Map<Long, List<MentionableUser>>,
  ): List<DueMention> {
    val mutedProjects =
      dueMentions
        .map { it.projectId }
        .distinct()
        .filter { mentionMuteService.isMuted(userId, it) }
        .toSet()
    val (mentions, revoked) =
      dueMentions.partition { mention ->
        mention.projectId !in mutedProjects &&
          mentionedByComment[mention.commentId].orEmpty().any { it.id == userId }
      }
    queries.deleteIfUnchanged(revoked.map { it.pending })
    return mentions
  }

  private fun narrowWorkAndDeleteUnreachable(
    userId: Long,
    work: List<PendingWork>,
  ): List<PendingWork> {
    val accessByProject =
      work
        .map { it.row.projectId }
        .distinct()
        .associateWith { projectAccess(it, userId) }
    val narrowed = work.associateWith { it.narrowedTo(accessByProject[it.row.projectId]) }
    queries.deleteIfUnchanged(narrowed.filterValues { it == null }.keys.map { it.pending })
    return narrowed.values.filterNotNull()
  }

  private class ProjectAccess(
    val member: ProjectMemberWithPermission,
    val nonBaseLanguageIds: List<Long>,
    val followedLanguageIds: Set<Long>?,
  )

  private fun projectAccess(
    projectId: Long,
    userId: Long,
  ): ProjectAccess? {
    val member = projectMemberPermissionProvider.findMembers(projectId, listOf(userId)).firstOrNull() ?: return null
    val nonBaseLanguageIds = languageService.findAll(projectId).filterNot { it.base }.map { it.id }
    return ProjectAccess(
      member = member,
      nonBaseLanguageIds = nonBaseLanguageIds,
      followedLanguageIds =
        projectPreferenceService.followedLanguages(projectId, listOf(member), nonBaseLanguageIds)[userId]?.languageIds,
    )
  }

  private fun PendingWork.narrowedTo(access: ProjectAccess?): PendingWork? {
    if (access == null) return null
    val actionableIds =
      NotificationEligibility.actionableLanguageIds(
        type = pending.type,
        permission = access.member.permission,
        languageIds = languages.map { it.id },
        nonBaseLanguageIds = access.nonBaseLanguageIds,
        followedLanguageIds = access.followedLanguageIds,
      ) ?: return null
    val actionable = languages.filter { it.id in actionableIds }
    if (actionable.size == languages.size) return this
    if (actionable.isEmpty()) return null
    return copy(
      languages = actionable,
      row = row.copy(count = actionable.sumOf { it.entityCount }, languageNames = actionable.map { it.name }),
    )
  }

  private fun findWorkIfWindowElapsed(userId: Long): List<PendingWork> {
    if (!workEmailWindowElapsed(userId)) return emptyList()
    return queries.findPendingWork(userId)
  }

  private fun workEmailWindowElapsed(userId: Long): Boolean {
    val state = digestStateService.find(listOf(userId))[userId] ?: return true
    val cutoff =
      when (state.frequency) {
        NotificationDigestFrequency.OFF -> return false
        NotificationDigestFrequency.DAILY -> dailyCutoff()
        NotificationDigestFrequency.SOON -> soonCutoff()
      }
    val lastSentAt = state.lastDigestSentAt ?: return true
    return !lastSentAt.after(cutoff)
  }

  private fun emailEnabledTypes(
    userId: Long,
    types: List<NotificationType>,
  ): Set<NotificationType> =
    types.distinct().filter { settingsService.isEnabled(userId, it, NotificationChannel.EMAIL) }.toSet()

  private fun workIfAnyDue(work: List<PendingWork>): List<PendingWork> {
    if (work.none { it.pending.isDue() }) return emptyList()
    return work
  }

  private fun PendingEmail.isDue(): Boolean = dueAt?.after(now()) == false

  private fun toMentionRows(
    mentions: List<DueMention>,
    mentionedByComment: Map<Long, List<MentionableUser>>,
  ): List<MentionRow> =
    mentions.map {
      MentionRow(
        notificationId = it.pending.notificationId,
        projectId = it.projectId,
        projectName = it.projectName,
        authorName = it.authorName,
        text = it.text,
        mentionedNames = mentionedByComment[it.commentId].orEmpty().associate { m -> m.id to m.name.orEmpty() },
        keyId = it.keyId,
        keyName = it.keyName,
        keyNamespace = it.keyNamespace,
        languageTag = it.languageTag,
        languageName = it.languageName,
      )
    }

  private fun messageId(
    userId: Long,
    sent: List<PendingEmail>,
  ): String {
    val newestUpdate = sent.maxOf { it.updatedAt.time }
    return "<digest-$userId-$newestUpdate@${messageIdHost()}>"
  }

  private fun messageIdHost(): String =
    tolgeeProperties.frontEndUrl
      ?.let { runCatching { URI(it).host }.getOrNull() }
      ?: smtpFromDomain()
      ?: "tolgee.io"

  private fun smtpFromDomain(): String? =
    tolgeeProperties.smtp.from
      ?.substringAfterLast('@', "")
      ?.takeWhile { it.isLetterOrDigit() || it == '.' || it == '-' }
      ?.takeIf { it.isNotEmpty() }

  private fun markSent(
    userId: Long,
    sent: List<PendingEmail>,
    sentWork: List<PendingWork>,
  ) {
    executeInNewTransaction(transactionManager) {
      queries.clearPendingAndDropEmailOnly(sent)
      queries.setEmailedCounts(sentWork)
      if (sentWork.isNotEmpty()) {
        digestStateService.markSent(userId, currentDateProvider.date)
      }
    }
  }

  private fun now() = Timestamp(currentDateProvider.date.time)

  private fun soonCutoff() = Timestamp(currentDateProvider.date.time - props.workEmailMinIntervalMs)

  private fun dailyCutoff() = Timestamp(currentDateProvider.date.time - props.dailyWorkEmailMinIntervalMs)

  companion object {
    private const val LOCK_NAME = "notification-email"
    private val LEASE_TIME = Duration.ofMinutes(10)
  }
}
