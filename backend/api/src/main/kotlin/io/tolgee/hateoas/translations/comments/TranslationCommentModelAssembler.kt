package io.tolgee.hateoas.translations.comments

import io.tolgee.api.v2.controllers.translation.TranslationCommentController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.translation.TranslationComment
import io.tolgee.service.translation.CommentMentionService
import io.tolgee.service.translation.MentionableUser
import org.springframework.data.domain.Page
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class TranslationCommentModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
  private val commentMentionService: CommentMentionService,
  private val mentionedUserModelAssembler: MentionedUserModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationComment, TranslationCommentModel>(
    TranslationCommentController::class.java,
    TranslationCommentModel::class.java,
  ) {
  override fun toModel(entity: TranslationComment): TranslationCommentModel =
    toModel(entity, commentMentionService.findMentionedMembers(entity))

  fun toPagedModel(
    comments: Page<TranslationComment>,
    pagedResourcesAssembler: PagedResourcesAssembler<TranslationComment>,
  ): PagedModel<TranslationCommentModel> {
    val mentionsByComment = commentMentionService.findMentionedMembers(comments.content)
    return pagedResourcesAssembler.toModel(
      comments,
      RepresentationModelAssembler { toModel(it, mentionsByComment[it.id].orEmpty()) },
    )
  }

  private fun toModel(
    entity: TranslationComment,
    mentions: List<MentionableUser>,
  ): TranslationCommentModel =
    TranslationCommentModel(
      id = entity.id,
      text = entity.text,
      state = entity.state,
      author = entity.author.let { simpleUserAccountModelAssembler.toModel(it) },
      createdAt = entity.createdAt ?: Date(),
      updatedAt = entity.updatedAt ?: Date(),
      mentions = mentions.map { mentionedUserModelAssembler.toModel(it) },
    )
}
