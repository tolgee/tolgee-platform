package io.tolgee.fixtures

import io.tolgee.constants.ApiScope
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.model.UserAccount
import io.tolgee.service.ApiKeyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
class ProjectApiKeyAuthRequestPerformer(
        private val userAccount: UserAccount,
        private val scopes: Array<ApiScope>,
        projectUrlPrefix: String = "/api/project"
) : ProjectAuthRequestPerformer(userAccount, projectUrlPrefix) {

    @field:Autowired
    lateinit var apiKeyService: ApiKeyService

    val apiKey: ApiKeyDTO by lazy {
        apiKeyService.createApiKey(userAccount, scopes = this.scopes.toSet(), project)
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

    private val String.withApiKey: String
        get() {
            val symbol = if (this.contains("?")) "&" else "?"
            return this + symbol + "ak=" + apiKey.key
        }
}
