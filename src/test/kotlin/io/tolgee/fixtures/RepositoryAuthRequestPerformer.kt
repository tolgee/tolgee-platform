package io.tolgee.fixtures

import io.tolgee.model.Repository
import org.springframework.test.web.servlet.ResultActions


interface RepositoryAuthRequestPerformer : AuthRequestPerformer {
    val repository: Repository

    companion object {
        const val API_REPOSITORY_URL_PREFIX = "/api/repository/"
    }

    fun performRepositoryAuthPut(url: String, content: Any?): ResultActions
    fun performRepositoryAuthPost(url: String, content: Any?): ResultActions
    fun performRepositoryAuthGet(url: String): ResultActions
    fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions
}
