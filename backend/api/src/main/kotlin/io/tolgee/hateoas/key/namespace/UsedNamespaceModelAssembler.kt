package io.tolgee.hateoas.key.namespace

import io.tolgee.api.v2.controllers.NamespaceController
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UsedNamespaceModelAssembler :
  RepresentationModelAssemblerSupport<Pair<Long?, String?>, UsedNamespaceModel>(
    NamespaceController::class.java,
    UsedNamespaceModel::class.java,
  ) {
  override fun toModel(entity: Pair<Long?, String?>) =
    UsedNamespaceModel(
      id = entity.first,
      name = entity.second,
    )
}
