package io.tolgee.controllers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import io.tolgee.AbstractTransactionalTest
import io.tolgee.ITest
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.RequestPerformer
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.OrganizationRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.security.payload.LoginRequest
import io.tolgee.service.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.io.UnsupportedEncodingException
import java.util.*

@SpringBootTest
abstract class AbstractControllerTest :
        AbstractTransactionalTest(), ITest, RequestPerformer {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected lateinit var mvc: MockMvc

    @Autowired
    protected lateinit var dbPopulator: DbPopulatorReal

    @Autowired
    protected lateinit var repositoryService: RepositoryService

    @Autowired
    protected lateinit var translationService: TranslationService

    @Autowired
    protected lateinit var keyService: KeyService

    @Autowired
    protected lateinit var languageService: LanguageService

    @Autowired
    protected lateinit var keyRepository: KeyRepository

    @Autowired
    protected lateinit var userAccountService: UserAccountService

    @Autowired
    protected lateinit var apiKeyService: ApiKeyService

    @Autowired
    protected lateinit var permissionService: PermissionService

    @Autowired
    protected lateinit var invitationService: InvitationService

    @Autowired
    protected lateinit var tolgeeProperties: TolgeeProperties

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    protected lateinit var initialPasswordManager: InitialPasswordManager

    @Autowired
    protected lateinit var screenshotService: ScreenshotService

    protected lateinit var initialUsername: String

    protected lateinit var initialPassword: String

    @Qualifier("baseRequestPerformer")
    @Autowired
    protected lateinit var requestPerformer: RequestPerformer

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    @Autowired
    protected lateinit var organizationService: OrganizationService

    @Autowired
    private fun initInitialUser(authenticationProperties: AuthenticationProperties) {
        initialUsername = authenticationProperties.initialUsername
        initialPassword = initialPasswordManager.initialPassword
    }

    fun <T> decodeJson(json: String?, clazz: Class<T>?): T {
        val mapper = ObjectMapper()
        return try {
            mapper.readValue(json, clazz)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    protected fun login(userName: String?, password: String?): DefaultAuthenticationResult {
        val response = doAuthentication(userName, password)
                .response.contentAsString
        val userAccount = userAccountService.getByUserName(userName).orElseThrow { NotFoundException() }
        return DefaultAuthenticationResult(mapper.readValue(response, HashMap::class.java)["accessToken"] as String?, userAccount)
    }

    protected fun doAuthentication(username: String?, password: String?): MvcResult {
        val request = LoginRequest()
        request.username = username
        request.password = password
        val jsonRequest = mapper.writeValueAsString(request)
        return mvc.perform(MockMvcRequestBuilders.post("/api/public/generatetoken")
                .content(jsonRequest)
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
    }

    protected fun <T> mapResponse(result: MvcResult, type: JavaType?): T {
        return try {
            mapper.readValue(result.response.contentAsString, type)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    protected fun <T> mapResponse(result: MvcResult, clazz: Class<T>?): T {
        return try {
            mapper.readValue(result.response.contentAsString, clazz)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    protected fun <C : Collection<E>?, E> mapResponse(result: MvcResult, collectionType: Class<C>?, elementType: Class<E>?): C {
        return try {
            mapper!!.readValue(
                    result.response.contentAsString,
                    TypeFactory.defaultInstance().constructCollectionType(collectionType, elementType)
            )
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
        return requestPerformer.perform(builder)
    }

    override fun performPut(url: String, content: Any?): ResultActions {
        return requestPerformer.performPut(url, content)
    }

    override fun performPost(url: String, content: Any?): ResultActions {
        return requestPerformer.performPost(url, content)
    }

    override fun performGet(url: String): ResultActions {
        return requestPerformer.performGet(url)
    }

    override fun performDelete(url: String, content: Any?): ResultActions {
        return requestPerformer.performDelete(url, content)
    }
}
