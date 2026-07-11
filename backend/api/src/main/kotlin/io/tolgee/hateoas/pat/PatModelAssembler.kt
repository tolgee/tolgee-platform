package io.tolgee.hateoas.pat

import io.tolgee.api.v2.controllers.PatController
import io.tolgee.model.Pat
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class PatModelAssembler :
  RepresentationModelAssemblerSupport<Pat, PatModel>(
    PatController::class.java,
    PatModel::class.java,
  ) {
  override fun toModel(entity: Pat): PatModel {
    return PatModel(
      id = entity.id,
      description = entity.description,
      expiresAt = entity.expiresAt?.time,
      createdAt = entity.createdAt?.time ?: Date().time,
      updatedAt = entity.updatedAt?.time ?: Date().time,
      lastUsedAt = entity.lastUsedAt?.time,
    )
  }
}
