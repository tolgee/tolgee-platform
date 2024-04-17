package io.tolgee.fixtures

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.dtos.response.ApiKeyDTO
import io.tolgee.model.UserAccount
import io.tolgee.service.security.ApiKeyService
import io.tolgee.testing.annotations.ApiKeyPresentMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
@org.springframework.context.annotation.Scope("prototype")
class ProjectApiKeyAuthRequestPerformer(
  private val userAccountProvider: () -> UserAccount,
  private val scopes: Array<io.tolgee.model.enums.Scope>,
  projectUrlPrefix: String = "/api/project",
  private val apiKeyPresentMode: ApiKeyPresentMode = ApiKeyPresentMode.HEADER,
) : ProjectAuthRequestPerformer(userAccountProvider, projectUrlPrefix) {
  @field:Autowired
  lateinit var apiKeyService: ApiKeyService

  val apiKey: ApiKeyDTO by lazy {
    ApiKeyDTO.fromEntity(
      apiKeyService.create(userAccountProvider.invoke(), scopes = this.scopes.toSet(), project),
    )
  }

  override fun performProjectAuthPut(
    url: String,
    content: Any?,
  ): ResultActions {
    return performPut(projectUrlPrefix + url.withApiKey, content, headersWithApiKey)
  }

  override fun performProjectAuthPost(
    url: String,
    content: Any?,
  ): ResultActions {
    return performPost(projectUrlPrefix + url.withApiKey, content, headersWithApiKey)
  }

  override fun performProjectAuthGet(url: String): ResultActions {
    return performGet(projectUrlPrefix + url.withApiKey, headersWithApiKey)
  }

  override fun performProjectAuthDelete(
    url: String,
    content: Any?,
  ): ResultActions {
    return performDelete(projectUrlPrefix + url.withApiKey, content, headersWithApiKey)
  }

  override fun performProjectAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>,
  ): ResultActions {
    val builder = MockMvcRequestBuilders.multipart(url)
    files.forEach { builder.file(it) }
    params.forEach { (name, values) -> builder.param(name, *values) }
    return mvc.perform(
      AuthorizedRequestFactory.addToken(
        MockMvcRequestBuilders.multipart(projectUrlPrefix + url.withApiKey),
      ),
    )
  }

  private val String.withApiKey: String
    get() {
      if (apiKeyPresentMode == ApiKeyPresentMode.QUERY_PARAM) {
        val symbol = if (this.contains("?")) "&" else "?"
        return this + symbol + "ak=" + apiKey.key
      }
      return this
    }

  private val headersWithApiKey: HttpHeaders
    get() {
      if (apiKeyPresentMode == ApiKeyPresentMode.HEADER) {
        return HttpHeaders().apply { add(API_KEY_HEADER_NAME, apiKey.key) }
      }
      return HttpHeaders.EMPTY
    }
}
