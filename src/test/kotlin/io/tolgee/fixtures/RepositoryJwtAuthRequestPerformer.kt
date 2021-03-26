package io.tolgee.fixtures

import io.tolgee.development.DbPopulatorReal
import io.tolgee.fixtures.RepositoryAuthRequestPerformer.Companion.API_REPOSITORY_URL_PREFIX
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

@Component
@Scope("prototype")
class RepositoryJwtAuthRequestPerformer(
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userAccount: UserAccount,
        dbPopulator: DbPopulatorReal,
) : RepositoryAuthRequestPerformer(userAccount) {

    override fun performRepositoryAuthPut(url: String, content: Any?): ResultActions {
        return super.performAuthPut(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }

    override fun performRepositoryAuthPost(url: String, content: Any?): ResultActions {
        return performPost(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }

    override fun performRepositoryAuthGet(url: String): ResultActions {
        return performGet(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url)
    }

    override fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions {
        return performDelete(API_REPOSITORY_URL_PREFIX + repository.id + "/" + url, content)
    }
}
