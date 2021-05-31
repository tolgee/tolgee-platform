package io.tolgee.fixtures

import io.tolgee.development.DbPopulatorReal
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions


abstract class RepositoryAuthRequestPerformer(userAccount: UserAccount) : SignedInRequestPerformer() {

    @field:Autowired
    lateinit var dbPopulator: DbPopulatorReal

    val project: Project by lazy {
        projectSupplier?.invoke()
                ?: dbPopulator.createBase(generateUniqueString(), username = userAccount.username!!)
    }

    var projectSupplier: (() -> Project)? = null

    companion object {
        const val API_REPOSITORY_URL_PREFIX = "/api/repository/"
    }

    abstract fun performRepositoryAuthPut(url: String, content: Any?): ResultActions
    abstract fun performRepositoryAuthPost(url: String, content: Any?): ResultActions
    abstract fun performRepositoryAuthGet(url: String): ResultActions
    abstract fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions
}
