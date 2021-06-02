package io.tolgee.fixtures

import io.tolgee.model.UserAccount
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions

@Component
@Scope("prototype")
class ProjectJwtAuthRequestPerformer(
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userAccount: UserAccount,
) : ProjectAuthRequestPerformer(userAccount) {

    override fun performProjectAuthPut(url: String, content: Any?): ResultActions {
        return super.performAuthPut(API_PROJECT_URL_PREFIX + project.id + "/" + url, content)
    }

    override fun performProjectAuthPost(url: String, content: Any?): ResultActions {
        return performAuthPost(API_PROJECT_URL_PREFIX + project.id + "/" + url, content)
    }

    override fun performProjectAuthGet(url: String): ResultActions {
        return performAuthGet(API_PROJECT_URL_PREFIX + project.id + "/" + url)
    }

    override fun performProjectAuthDelete(url: String, content: Any?): ResultActions {
        return performAuthDelete(API_PROJECT_URL_PREFIX + project.id + "/" + url, content)
    }
}
