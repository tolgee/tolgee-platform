package io.tolgee.hateoas.apiKey

import io.tolgee.api.v2.controllers.ApiKeyController
import io.tolgee.model.ApiKey
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ApiKeyModelAssembler :
  RepresentationModelAssemblerSupport<ApiKey, ApiKeyModel>(
    ApiKeyController::class.java,
    ApiKeyModel::class.java,
  ) {
  override fun toModel(entity: ApiKey) =
    ApiKeyModel(
      id = entity.id,
      description = entity.description,
      username = entity.userAccount.username,
      userFullName = entity.userAccount.name,
      projectId = entity.project.id,
      projectName = entity.project.name,
      scopes = entity.scopesEnum.mapNotNull { it?.value }.toSet(),
      expiresAt = entity.expiresAt?.time,
      lastUsedAt = entity.lastUsedAt?.time,
    )
}
