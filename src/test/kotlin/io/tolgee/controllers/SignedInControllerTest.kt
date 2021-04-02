package io.tolgee.controllers

import io.tolgee.fixtures.AuthRequestPerformer
import io.tolgee.fixtures.LoggedRequestFactory.init
import io.tolgee.fixtures.SignedInRequestPerformer
import io.tolgee.model.UserAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod

@SpringBootTest
@AutoConfigureMockMvc
abstract class SignedInControllerTest : AbstractControllerTest(), AuthRequestPerformer {
    var userAccount: UserAccount? = null

    @Autowired
    lateinit var signedInRequestPerformer: SignedInRequestPerformer

    @BeforeMethod
    fun beforeEach() {
        //populate to create the user if not created
        dbPopulator.createUserIfNotExists(tolgeeProperties.authentication.initialUsername)
        if (userAccount == null) {
            logAsUser(tolgeeProperties.authentication.initialUsername, initialPassword)
        }
        commitTransaction()
    }

    @AfterMethod
    fun afterEach() {
        logout()
    }

    fun logAsUser(userName: String, password: String) {
        val defaultAuthenticationResult = login(userName, password)
        init(defaultAuthenticationResult.token)
        userAccount = defaultAuthenticationResult.entity
    }

    fun logout() {
        userAccount = null
    }

    override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
        return requestPerformer.perform(builder)
    }

    override fun performDelete(url: String, content: Any?): ResultActions {
        return requestPerformer.performDelete(url, content)
    }

    override fun performGet(url: String): ResultActions {
        return requestPerformer.performGet(url)
    }

    override fun performPost(url: String, content: Any?): ResultActions {
        return requestPerformer.performPost(url, content)
    }

    override fun performPut(url: String, content: Any?): ResultActions {
        return requestPerformer.performPut(url, content)
    }

    override fun performAuthPut(url: String, content: Any?): ResultActions {
        return signedInRequestPerformer.performAuthPut(url, content)
    }

    override fun performAuthPost(url: String, content: Any?): ResultActions {
        return signedInRequestPerformer.performAuthPost(url, content)
    }

    override fun performAuthGet(url: String): ResultActions {
        return signedInRequestPerformer.performAuthGet(url)
    }

    override fun performAuthDelete(url: String, content: Any?): ResultActions {
        return signedInRequestPerformer.performAuthDelete(url, content)
    }
}
