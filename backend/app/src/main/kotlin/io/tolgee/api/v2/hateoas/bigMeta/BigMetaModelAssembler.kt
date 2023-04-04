package io.tolgee.api.v2.hateoas.bigMeta

import io.tolgee.api.v2.controllers.V2LanguagesController
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.api.v2.hateoas.language.BigMetaModel
import io.tolgee.model.views.BigMetaView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class BigMetaModelAssembler(
  private val keyModelAssembler: KeyModelAssembler,
) : RepresentationModelAssemblerSupport<BigMetaView, BigMetaModel>(
  V2LanguagesController::class.java, BigMetaModel::class.java
) {
  override fun toModel(view: BigMetaView): BigMetaModel {
    return BigMetaModel(
      id = view.bigMeta.id,
      namespace = view.bigMeta.namespace,
      keyName = view.bigMeta.keyName,
      location = view.bigMeta.location,
      key = view.key?.let { keyModelAssembler.toModel(it) },
      contextData = view.bigMeta.contextData
    )
  }
}
