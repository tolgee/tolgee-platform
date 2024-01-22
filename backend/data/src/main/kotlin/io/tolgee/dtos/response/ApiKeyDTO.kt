package io.tolgee.dtos.response

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.ApiKey
import io.tolgee.security.PROJECT_API_KEY_PREFIX

class ApiKeyDTO(
  var id: Long = 0,
  @field:Schema(
    description =
      "Resulting user's api key. " +
        "Is is hidden when is not response to creation or regeneration.",
  )
  var key: String? = null,
  var description: String = "",
  var userName: String? = null,
  var projectId: Long = 0,
  var projectName: String = "",
  var scopes: Set<String> = setOf(),
) {
  companion object {
    fun fromEntity(apiKey: ApiKey): ApiKeyDTO {
      return ApiKeyDTO(
        key = apiKey.encodedKey?.let { PROJECT_API_KEY_PREFIX + it },
        id = apiKey.id,
        description = apiKey.description,
        userName = apiKey.userAccount.name,
        projectId = apiKey.project.id,
        projectName = apiKey.project.name,
        scopes = apiKey.scopesEnum.mapNotNull { it?.value }.toSet(),
      )
    }
  }
}
