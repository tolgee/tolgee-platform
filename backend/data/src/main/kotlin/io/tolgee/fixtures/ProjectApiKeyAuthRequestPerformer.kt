package io.tolgee.fixtures

import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
class ProjectApiKeyAuthRequestPerformer(
  private val userAccountProvider: () -> UserAccount,
  private val scopes: Array<ApiScope>,
  projectUrlPrefix: String = "/api/project"
) : ProjectAuthRequestPerformer(userAccountProvider, projectUrlPrefix) {

  @field:Autowired
  lateinit var apiKeyService: io.tolgee.service.ApiKeyService

  val apiKey: ApiKeyDTO by lazy {
    ApiKeyDTO.fromEntity(
      apiKeyService.create(userAccountProvider.invoke(), scopes = this.scopes.toSet(), project)
    )
  }

  override fun performProjectAuthPut(url: String, content: Any?): ResultActions {
    return performPut(projectUrlPrefix + url.withApiKey, content)
  }

  override fun performProjectAuthPost(url: String, content: Any?): ResultActions {
    return performPost(projectUrlPrefix + url.withApiKey, content)
  }

  override fun performProjectAuthGet(url: String): ResultActions {
    return performGet(projectUrlPrefix + url.withApiKey)
  }

  override fun performProjectAuthDelete(url: String, content: Any?): ResultActions {
    return performDelete(projectUrlPrefix + url.withApiKey, content)
  }

  override fun performProjectAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>
  ): ResultActions {
    val builder = MockMvcRequestBuilders.multipart(url)
    files.forEach { builder.file(it) }
    params.forEach { (name, values) -> builder.param(name, *values) }
    return mvc.perform(
      LoggedRequestFactory.addToken(
        MockMvcRequestBuilders.multipart(projectUrlPrefix + url.withApiKey)
      )
    )
  }

  private val String.withApiKey: String
    get() {
      val symbol = if (this.contains("?")) "&" else "?"
      return this + symbol + "ak=" + apiKey.key
    }
}
