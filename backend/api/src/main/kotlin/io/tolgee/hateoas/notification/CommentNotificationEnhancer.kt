package io.tolgee.hateoas.notification

import io.tolgee.hateoas.translations.comments.MentionedUserModelAssembler
import io.tolgee.model.notifications.Notification
import io.tolgee.model.translation.TranslationComment
import io.tolgee.service.translation.CommentMentionService
import io.tolgee.service.translation.MentionableUser
import io.tolgee.service.translation.TranslationCommentService
import org.springframework.stereotype.Component

@Component
class CommentNotificationEnhancer(
  private val translationCommentService: TranslationCommentService,
  private val commentMentionService: CommentMentionService,
  private val mentionedUserModelAssembler: MentionedUserModelAssembler,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val commentIds = notifications.keys.mapNotNull { it.linkedComment?.id }.distinct()
    if (commentIds.isEmpty()) return
    val comments = translationCommentService.findAllWithKeyAndLanguage(commentIds).associateBy { it.id }
    val mentionsByComment = commentMentionService.findMentionedMembers(comments.values)

    notifications.forEach { (source, target) ->
      val comment = source.linkedComment?.id?.let { comments[it] } ?: return@forEach
      target.linkedComment = toModel(comment, mentionsByComment[comment.id].orEmpty())
    }
  }

  private fun toModel(
    comment: TranslationComment,
    mentions: List<MentionableUser>,
  ): LinkedCommentModel {
    val translation = comment.translation
    val key = translation.key
    return LinkedCommentModel(
      id = comment.id,
      text = comment.text,
      mentions = mentions.map { mentionedUserModelAssembler.toModel(it) },
      keyId = key.id,
      keyName = key.name,
      keyNamespace = key.namespace?.name,
      languageId = translation.language.id,
      languageTag = translation.language.tag,
      translationId = translation.id,
    )
  }
}
