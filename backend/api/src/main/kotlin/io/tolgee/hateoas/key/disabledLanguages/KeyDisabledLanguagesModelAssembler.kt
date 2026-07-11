package io.tolgee.hateoas.key.disabledLanguages

import io.tolgee.dtos.queryResults.keyDisabledLanguages.KeyDisabledLanguagesView
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class KeyDisabledLanguagesModelAssembler :
  RepresentationModelAssembler<KeyDisabledLanguagesView, KeyDisabledLanguagesModel> {
  override fun toModel(view: KeyDisabledLanguagesView): KeyDisabledLanguagesModel {
    return KeyDisabledLanguagesModel(
      id = view.id,
      name = view.name,
      namespace = view.namespace,
      disabledLanguages =
        view.disabledLanguages.map {
          KeyDisabledLanguageModel(
            id = it.id,
            tag = it.tag,
          )
        },
    )
  }
}
