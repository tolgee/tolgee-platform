package io.tolgee.dtos.response.ApiKeyDTO

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.ApiKey
import io.tolgee.model.enums.ApiScope
import java.util.stream.Collectors

class ApiKeyDTO(
  var id: Long = 0,
  @field:Schema(description = "Resulting user's api key")
  var key: String = "",
  var userName: String? = null,
  var projectId: Long = 0,
  var projectName: String = "",
  var scopes: Set<String> = setOf()
) {

  companion object {
    fun fromEntity(apiKey: ApiKey): ApiKeyDTO {
      return ApiKeyDTO(
        key = apiKey.key,
        id = apiKey.id,
        userName = apiKey.userAccount.name,
        projectId = apiKey.project.id,
        projectName = apiKey.project.name,
        scopes = apiKey.scopesEnum.stream().map(ApiScope::value).collect(Collectors.toSet())
      )
    }
  }
}
