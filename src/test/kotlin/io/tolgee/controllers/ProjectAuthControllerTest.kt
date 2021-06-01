package io.tolgee.controllers

import io.tolgee.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.fixtures.AuthRequestPerformer
import io.tolgee.fixtures.RepositoryApiKeyAuthRequestPerformer
import io.tolgee.fixtures.RepositoryAuthRequestPerformer
import io.tolgee.fixtures.RepositoryJwtAuthRequestPerformer
import io.tolgee.model.Project
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.testng.annotations.BeforeMethod
import java.lang.reflect.Method


@SpringBootTest
@AutoConfigureMockMvc
abstract class ProjectAuthControllerTest : SignedInControllerTest(), AuthRequestPerformer {

    //for api key auth methods
    val apiKey: ApiKeyDTO
        get() {
            val performer = this.projectAuthRequestPerformer
            return if (performer is RepositoryApiKeyAuthRequestPerformer)
                performer.apiKey else
                throw Exception("Method not annotated with ApiKeyAccessTestMethod?")
        }

    val project: Project
        get() = this.projectAuthRequestPerformer.project

    var projectSupplier: (() -> Project)?
        get() = this.projectAuthRequestPerformer.projectSupplier
        set(value) {
            this.projectAuthRequestPerformer.projectSupplier = value
        }

    private var _projectAuthRequestPerformer: RepositoryAuthRequestPerformer? = null;

    private var projectAuthRequestPerformer: RepositoryAuthRequestPerformer
        get() {
            return _projectAuthRequestPerformer
                    ?: throw Exception("Method not annotated with ApiKeyAccessTestMethod nor RepositoryJWTAuthTestMethod?")
        }
        set(value) {
            _projectAuthRequestPerformer = value
        }

    @BeforeMethod
    fun beforeEach(method: Method) {
        with(method.getAnnotation(ProjectApiKeyAuthTestMethod::class.java)) {
            if (this != null) {
                this@ProjectAuthControllerTest.projectAuthRequestPerformer =
                        applicationContext!!.getBean(RepositoryApiKeyAuthRequestPerformer::class.java, userAccount, this.scopes)
            }
        }

        with(method.getAnnotation(ProjectJWTAuthTestMethod::class.java)) {
            if (this != null) {
                this@ProjectAuthControllerTest.projectAuthRequestPerformer =
                        applicationContext!!.getBean(RepositoryJwtAuthRequestPerformer::class.java, userAccount)
            }
        }
    }

    fun performRepositoryAuthPut(url: String, content: Any?): ResultActions {
        return projectAuthRequestPerformer.performRepositoryAuthPut(url, content)
    }

    fun performRepositoryAuthPost(url: String, content: Any?): ResultActions {
        return projectAuthRequestPerformer.performRepositoryAuthPost(url, content)
    }

    fun performRepositoryAuthGet(url: String): ResultActions {
        return projectAuthRequestPerformer.performRepositoryAuthGet(url)
    }

    fun performRepositoryAuthDelete(url: String, content: Any?): ResultActions {
        return projectAuthRequestPerformer.performRepositoryAuthDelete(url, content)
    }

}
