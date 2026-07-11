package io.tolgee.hateoas.translations.suggestions

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.views.TranslationSuggestionView
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationSuggestionSimpleModelAssembler(
  private val avatarService: AvatarService,
) : RepresentationModelAssemblerSupport<TranslationSuggestionView, TranslationSuggestionSimpleModel>(
    TranslationsController::class.java,
    TranslationSuggestionSimpleModel::class.java,
  ) {
  override fun toModel(entity: TranslationSuggestionView): TranslationSuggestionSimpleModel {
    return TranslationSuggestionSimpleModel(
      id = entity.id,
      translation = entity.translation,
      author =
        SimpleUserAccountModel(
          id = entity.authorId,
          name = entity.authorName,
          username = entity.authorUsername,
          avatar = avatarService.getAvatarLinks(entity.authorAvatarHash),
          deleted = entity.authorDeletedAt != null,
        ),
      state = entity.state,
      isPlural = entity.plural,
    )
  }
}
