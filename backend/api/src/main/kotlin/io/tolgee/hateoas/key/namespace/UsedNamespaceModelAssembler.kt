package io.tolgee.hateoas.key.namespace

import io.tolgee.api.v2.controllers.NamespaceController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UsedNamespaceModelAssembler : RepresentationModelAssemblerSupport<Pair<Pair<Long?, String?>, Boolean?>, UsedNamespaceModel>(
  NamespaceController::class.java,
  UsedNamespaceModel::class.java,
) {
  override fun toModel(entity: Pair<Pair<Long?, String?>, Boolean?>) =
    UsedNamespaceModel(
      id = entity.first.first,
      name = entity.first.second,
      base = entity.second,
    )
}
