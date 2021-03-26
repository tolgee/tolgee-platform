package io.tolgee.fixtures

import io.tolgee.development.DbPopulatorReal
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
import io.tolgee.service.ApiKeyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Supplier


abstract class RepositoryAuthRequestPerformer(userAccount: UserAccount) : SignedInRequestPerformer() {

    @field:Autowired
    lateinit var dbPopulator: DbPopulatorReal

    val repository: Repository by lazy {
        repositorySupplier?.invoke()
                ?: dbPopulator.createBase(generateUniqueString(), username = userAccount.username!!)
    }

    var repositorySupplier: (() -> Repository)? = null

    companion object {
        const val API_REPOSITORY_URL_PREFIX = "/api/repository/"
    }

    abstract fun performRepositoryAuthPut(url: String, content: Any?): ResultActions
    abstract fun performRepositoryAuthPost(url: String, content: Any?): ResultActions
    abstract fun performRepositoryAuthGet(url: String): ResultActions
    abstract fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions
}
