package io.tolgee.fixtures

import io.tolgee.model.UserAccount
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

@Component
@Scope("prototype")
class RepositoryJwtAuthRequestPerformer(
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userAccount: UserAccount,
) : RepositoryAuthRequestPerformer(userAccount) {

    override fun performRepositoryAuthPut(url: String, content: Any?): ResultActions {
        return super.performAuthPut(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }

    override fun performRepositoryAuthPost(url: String, content: Any?): ResultActions {
        return performAuthPost(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }

    override fun performRepositoryAuthGet(url: String): ResultActions {
        return performAuthGet(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url)
    }

    override fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions {
        return performAuthDelete(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }
}
