package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeKeyModel
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeTranslationModel
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import org.springframework.stereotype.Component

@Component
class BranchMergeKeyModelAssembler {
  fun toModel(
    key: Key,
    allowedLanguageTags: Set<String>?,
  ): BranchMergeKeyModel {
    val translationsByTag = key.translations.associateBy { it.language.tag }
    val translationsByLang =
      if (allowedLanguageTags == null) {
        translationsByTag.mapValues { (_, translation) ->
          BranchMergeTranslationModel(
            id = translation.id,
            language = translation.language.tag,
            text = translation.text,
            state = translation.state,
            outdated = translation.outdated,
          )
        }
      } else {
        allowedLanguageTags.associateWith { tag ->
          val translation = translationsByTag[tag]
          BranchMergeTranslationModel(
            id = translation?.id,
            language = tag,
            text = translation?.text,
            state = translation?.state ?: TranslationState.UNTRANSLATED,
            outdated = translation?.outdated ?: false,
          )
        }
      }

    return BranchMergeKeyModel(
      keyId = key.id,
      keyName = key.name,
      keyIsPlural = key.isPlural,
      keyDescription = key.keyMeta?.description,
      translations = translationsByLang,
      key.namespace?.name,
    )
  }
}
