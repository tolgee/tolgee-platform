package io.tolgee.hateoas.translations.comments

import io.tolgee.api.v2.controllers.translation.TranslationCommentController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.translation.TranslationComment
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class TranslationCommentModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationComment, TranslationCommentModel>(
    TranslationCommentController::class.java,
    TranslationCommentModel::class.java,
  ) {
  override fun toModel(entity: TranslationComment): TranslationCommentModel {
    return TranslationCommentModel(
      id = entity.id,
      text = entity.text,
      state = entity.state,
      author = entity.author.let { simpleUserAccountModelAssembler.toModel(it) },
      createdAt = entity.createdAt ?: Date(),
      updatedAt = entity.updatedAt ?: Date(),
    )
  }
}
