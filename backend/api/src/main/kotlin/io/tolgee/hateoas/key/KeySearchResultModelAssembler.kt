package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.keys.KeyController
import io.tolgee.service.key.KeySearchResultView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeySearchResultModelAssembler :
  RepresentationModelAssemblerSupport<KeySearchResultView, KeySearchSearchResultModel>(
    KeyController::class.java,
    KeySearchSearchResultModel::class.java,
  ) {
  override fun toModel(view: KeySearchResultView): KeySearchSearchResultModel {
    return KeySearchSearchResultModel(view)
  }
}
