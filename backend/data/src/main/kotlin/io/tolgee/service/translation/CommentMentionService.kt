package io.tolgee.service.translation

import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.translation.TranslationComment
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.service.notification.NotificationMentionMuteService
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.security.ProjectMemberPermissionProvider
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

data class CommentMentionSource(
  val commentId: Long,
  val projectId: Long,
  val keyId: Long,
  val languageId: Long,
  val text: String?,
)

@Service
class CommentMentionService(
  private val projectMemberPermissionProvider: ProjectMemberPermissionProvider,
  private val threadParticipants: CommentThreadParticipants,
  private val mentionMuteService: NotificationMentionMuteService,
  @Lazy
  private val userAccountService: UserAccountService,
  @Lazy
  private val notificationService: NotificationService,
  private val entityManager: EntityManager,
) {
  fun parseMentionedUserIds(text: String?): Set<Long> {
    if (text.isNullOrEmpty()) return emptySet()
    return MENTION_REGEX
      .findAll(text)
      .mapNotNull { it.groupValues[1].toLongOrNull() }
      .toCollection(LinkedHashSet())
  }

  fun findMentionableUsers(
    projectId: Long,
    keyId: Long?,
    languageId: Long,
    exceptUserId: Long,
    search: String?,
    pageable: Pageable,
  ): Page<MentionableUser> {
    val unsorted = PageRequest.of(pageable.pageNumber, pageable.pageSize)
    val needed = unsorted.offset + unsorted.pageSize
    val participants =
      keyId
        ?.let { threadParticipants.find(projectId, it, languageId, exceptUserId, search?.trim()?.ifEmpty { null }) }
        .orEmpty()
    val participantIds = participants.map { it.id }.toSet()
    val batchSize = maxOf(unsorted.pageSize, MIN_CANDIDATE_BATCH)
    val collected = participants.toMutableList()
    var candidatePage = 0
    while (collected.size <= needed) {
      val candidates =
        userAccountService.findInProjectByName(
          projectId,
          search,
          exceptUserId,
          PageRequest.of(candidatePage, batchSize),
        )
      collected +=
        projectMemberPermissionProvider
          .withPermissions(projectId, candidates.content)
          .filter { it.canViewLanguage(languageId) && it.member.id !in participantIds }
          .map { it.member.toMentionable() }
      if (!candidates.hasNext()) break
      candidatePage++
    }
    val content = collected.drop(unsorted.offset.toInt()).take(unsorted.pageSize)
    return PageImpl(content, unsorted, collected.size.toLong())
  }

  fun findMentionedMembers(comment: TranslationComment): List<MentionableUser> =
    findMentionedMembers(listOf(comment))[comment.id].orEmpty()

  fun findMentionedMembers(comments: Collection<TranslationComment>): Map<Long, List<MentionableUser>> =
    findMentionedMembersBySource(
      comments
        .filter { parseMentionedUserIds(it.text).isNotEmpty() }
        .map {
          CommentMentionSource(
            commentId = it.id,
            projectId = it.translation.key.project.id,
            keyId = it.translation.key.id,
            languageId = it.translation.language.id,
            text = it.text,
          )
        },
    )

  fun findMentionedMembersBySource(sources: Collection<CommentMentionSource>): Map<Long, List<MentionableUser>> =
    sources
      .groupBy { it.projectId }
      .flatMap { (projectId, ofProject) -> findMentionedMembersInProject(projectId, ofProject).entries }
      .associate { it.key to it.value }

  private fun findMentionedMembersInProject(
    projectId: Long,
    sources: List<CommentMentionSource>,
  ): Map<Long, List<MentionableUser>> {
    val idsBySource = sources.associateWith { parseMentionedUserIds(it.text) }
    val members =
      projectMemberPermissionProvider
        .findMembers(projectId, onlyUserIds = idsBySource.values.flatten().toSet())
        .associateBy { it.member.id }
    return idsBySource.entries.associate { (source, ids) ->
      val viewingMembers =
        ids
          .mapNotNull { id -> members[id]?.takeIf { it.canViewLanguage(source.languageId) } }
          .associate { it.member.id to it.member.toMentionable() }
      val participants =
        threadParticipants
          .findAmong(projectId, source.keyId, source.languageId, ids.filter { it !in viewingMembers })
          .associateBy { it.id }
      source.commentId to ids.mapNotNull { viewingMembers[it] ?: participants[it] }
    }
  }

  fun notifyMentionedUsersOfNewComment(comment: TranslationComment) {
    notifyNewlyMentioned(comment, findMentionedUserIds(comment), alreadyMentioned = emptySet())
  }

  fun updateMentionNotificationsOfEditedComment(
    comment: TranslationComment,
    previousText: String?,
  ) {
    val mentioned = findMentionedUserIds(comment)
    notificationService.deleteUnseenMentionsOfCommentExcept(comment.id, mentioned + comment.author.id)
    notifyNewlyMentioned(comment, mentioned, alreadyMentioned = parseMentionedUserIds(previousText))
  }

  private fun findMentionedUserIds(comment: TranslationComment) = findMentionedMembers(comment).map { it.id }

  private fun notifyNewlyMentioned(
    comment: TranslationComment,
    mentioned: List<Long>,
    alreadyMentioned: Set<Long>,
  ) {
    val newlyMentioned = mentioned.filter { it !in alreadyMentioned && it != comment.author.id }
    val muted = mentionMuteService.mutedUserIds(comment.translation.key.project.id, newlyMentioned)
    newlyMentioned
      .filter { it !in muted }
      .forEach { userId -> notifyMentioned(comment, userId) }
  }

  private fun notifyMentioned(
    comment: TranslationComment,
    userId: Long,
  ) {
    notificationService.notify(
      Notification().apply {
        user = entityManager.getReference(UserAccount::class.java, userId)
        project = comment.translation.key.project
        type = NotificationType.COMMENT_MENTION
        originatingUser = comment.author
        linkedComment = comment
      },
    )
  }

  private fun UserAccountInProjectView.toMentionable() = MentionableUser(id, name, avatarHash)

  companion object {
    val MENTION_REGEX = Regex("<@(\\d+)>")
    private const val MIN_CANDIDATE_BATCH = 50
  }
}
