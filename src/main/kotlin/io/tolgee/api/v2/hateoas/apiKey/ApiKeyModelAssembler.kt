package io.tolgee.api.v2.hateoas.apiKey

import io.tolgee.api.v2.controllers.V2ApiKeyController
import io.tolgee.model.ApiKey
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ApiKeyModelAssembler : RepresentationModelAssemblerSupport<ApiKey, ApiKeyModel>(
  V2ApiKeyController::class.java, ApiKeyModel::class.java
) {
  override fun toModel(entity: ApiKey) = ApiKeyModel(
    id = entity.id,
    key = entity.key,
    username = entity.userAccount.username,
    userFullName = entity.userAccount.name,
    projectId = entity.project.id,
    projectName = entity.project.name,
    scopes = entity.scopesEnum.map { it.value }.toSet()
  )
}
