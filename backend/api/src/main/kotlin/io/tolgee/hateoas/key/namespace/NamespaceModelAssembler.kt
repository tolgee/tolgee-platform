package io.tolgee.hateoas.key.namespace

import io.tolgee.api.v2.controllers.NamespaceController
import io.tolgee.model.key.Namespace
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class NamespaceModelAssembler :
  RepresentationModelAssemblerSupport<Namespace, NamespaceModel>(
    NamespaceController::class.java,
    NamespaceModel::class.java,
  ) {
  override fun toModel(entity: Namespace) =
    NamespaceModel(
      id = entity.id,
      name = entity.name,
    )
}
