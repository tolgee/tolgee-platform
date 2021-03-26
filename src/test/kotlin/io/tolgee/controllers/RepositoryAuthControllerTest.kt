package io.tolgee.controllers

import io.tolgee.annotations.RepositoryApiKeyAuthTestMethod
import io.tolgee.annotations.RepositoryJWTAuthTestMethod
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.fixtures.*
import io.tolgee.model.Repository
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.testng.annotations.BeforeMethod
import java.lang.reflect.Method


@SpringBootTest
@AutoConfigureMockMvc
abstract class RepositoryAuthControllerTest : SignedInControllerTest(), AuthRequestPerformer {

    //for api key auth methods
    val apiKey: ApiKeyDTO
        get() {
            val performer = this.repositoryAuthRequestPerformer
            return if (performer is RepositoryApiKeyAuthRequestPerformer)
                performer.apiKey else
                throw Exception("Method not annotated with ApiKeyAccessTestMethod?")
        }

    val repository: Repository
        get() = this.repositoryAuthRequestPerformer.repository

    var repositorySupplier: (() -> Repository)?
        get() = this.repositoryAuthRequestPerformer.repositorySupplier
        set(value) {
            this.repositoryAuthRequestPerformer.repositorySupplier = value
        }

    private var _repositoryAuthRequestPerformer: RepositoryAuthRequestPerformer? = null;

    private var repositoryAuthRequestPerformer: RepositoryAuthRequestPerformer
        get() {
            return _repositoryAuthRequestPerformer
                    ?: throw Exception("Method not annotated with ApiKeyAccessTestMethod nor RepositoryJWTAuthTestMethod?")
        }
        set(value) {
            _repositoryAuthRequestPerformer = value
        }

    @BeforeMethod
    fun beforeEach(method: Method) {
        with(method.getAnnotation(RepositoryApiKeyAuthTestMethod::class.java)) {
            if (this != null) {
                this@RepositoryAuthControllerTest.repositoryAuthRequestPerformer =
                        applicationContext!!.getBean(RepositoryApiKeyAuthRequestPerformer::class.java, userAccount, this.scopes)
            }
        }

        with(method.getAnnotation(RepositoryJWTAuthTestMethod::class.java)) {
            if (this != null) {
                this@RepositoryAuthControllerTest.repositoryAuthRequestPerformer =
                        applicationContext!!.getBean(RepositoryJwtAuthRequestPerformer::class.java, userAccount)
            }
        }
    }

    fun performRepositoryAuthPut(url: String, content: Any?): ResultActions {
        return repositoryAuthRequestPerformer.performRepositoryAuthPut(url, content)
    }

    fun performRepositoryAuthPost(url: String, content: Any?): ResultActions {
        return repositoryAuthRequestPerformer.performRepositoryAuthPost(url, content)
    }

    fun performRepositoryAuthGet(url: String): ResultActions {
        return repositoryAuthRequestPerformer.performRepositoryAuthGet(url)
    }

    fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions {
        return repositoryAuthRequestPerformer.performRepositoryAuthDelete(url, content)
    }

}
