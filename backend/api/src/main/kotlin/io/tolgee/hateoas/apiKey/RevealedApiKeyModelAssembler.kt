package io.tolgee.hateoas.apiKey

import io.tolgee.api.v2.controllers.ApiKeyController
import io.tolgee.model.ApiKey
import io.tolgee.security.PROJECT_API_KEY_PREFIX
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class RevealedApiKeyModelAssembler(
  private val apiKeyModelAssembler: ApiKeyModelAssembler,
) : RepresentationModelAssemblerSupport<ApiKey, RevealedApiKeyModel>(
    ApiKeyController::class.java,
    RevealedApiKeyModel::class.java,
  ) {
  override fun toModel(entity: ApiKey) =
    RevealedApiKeyModel(
      apiKeyModel = apiKeyModelAssembler.toModel(entity),
      key =
        entity.encodedKey?.let { PROJECT_API_KEY_PREFIX + it }
          ?: throw IllegalStateException("Api key not present"),
    )
}
