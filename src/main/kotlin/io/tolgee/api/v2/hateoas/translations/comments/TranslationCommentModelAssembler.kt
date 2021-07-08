package io.tolgee.api.v2.hateoas.translations.comments

import io.tolgee.api.v2.controllers.translation.TranslationCommentController
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.model.translation.TranslationComment
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.*

@Component
class TranslationCommentModelAssembler(
  private val userAccountModelAssembler: UserAccountModelAssembler
) : RepresentationModelAssemblerSupport<TranslationComment, TranslationCommentModel>(
  TranslationCommentController::class.java, TranslationCommentModel::class.java
) {
  override fun toModel(entity: TranslationComment): TranslationCommentModel {
    return TranslationCommentModel(
      id = entity.id,
      text = entity.text,
      state = entity.state,
      author = entity.author.let { userAccountModelAssembler.toModel(it) },
      createdAt = entity.createdAt ?: Date(),
      updatedAt = entity.updatedAt ?: Date()
    )
  }
}
