package io.tolgee.hateoas.key.trash

import io.tolgee.api.v2.controllers.keys.KeyTrashController
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.hateoas.translations.TranslationViewModel
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Translation
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.util.addDays
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TrashedKeyWithTranslationsModelAssembler(
  private val tagModelAssembler: TagModelAssembler,
) : RepresentationModelAssemblerSupport<KeySearchResultView, TrashedKeyWithTranslationsModel>(
    KeyTrashController::class.java,
    TrashedKeyWithTranslationsModel::class.java,
  ) {
  var translationsByKeyId: Map<Long, List<Translation>> = emptyMap()
  var tagsByKeyId: Map<Long, List<Tag>> = emptyMap()

  override fun toModel(entity: KeySearchResultView): TrashedKeyWithTranslationsModel {
    val deletedAt = entity.deletedAt!!
    val tags = tagsByKeyId[entity.id]?.map { tagModelAssembler.toModel(it) } ?: emptyList()
    val translations = translationsByKeyId[entity.id] ?: emptyList()
    val translationsMap =
      translations.associate { translation ->
        translation.language.tag to
          TranslationViewModel(
            id = translation.id,
            text = translation.text,
            state = translation.state,
            auto = translation.auto,
            outdated = translation.outdated,
            mtProvider = translation.mtProvider,
            commentCount = 0,
            unresolvedCommentCount = 0,
            labels = null,
            activeSuggestionCount = 0,
            totalSuggestionCount = 0,
          )
      }

    return TrashedKeyWithTranslationsModel(
      id = entity.id,
      name = entity.name,
      namespace = entity.namespace,
      deletedAt = deletedAt,
      permanentDeleteAt = deletedAt.addDays(RETENTION_DAYS),
      description = entity.description,
      tags = tags,
      translations = translationsMap,
    )
  }

  companion object {
    private const val RETENTION_DAYS = 7
  }
}
