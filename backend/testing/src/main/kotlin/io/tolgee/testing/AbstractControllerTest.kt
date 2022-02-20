package io.tolgee.testing

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import io.tolgee.AbstractSpringTest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.RequestPerformer
import io.tolgee.security.payload.LoginRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.io.UnsupportedEncodingException

@AutoConfigureMockMvc
abstract class AbstractControllerTest :
  AbstractSpringTest(), RequestPerformer {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  protected lateinit var mvc: MockMvc

  @Qualifier("baseRequestPerformer")
  @Autowired
  protected lateinit var requestPerformer: RequestPerformer

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
    val userAccount = userAccountService.findOptional(userName).orElseThrow { NotFoundException() }
    return DefaultAuthenticationResult(
      mapper.readValue(response, HashMap::class.java)["accessToken"] as String, userAccount
    )
  }

  protected fun doAuthentication(username: String?, password: String?): MvcResult {
    val request = LoginRequest()
    request.username = username
    request.password = password
    val jsonRequest = mapper.writeValueAsString(request)
    return mvc.perform(
      MockMvcRequestBuilders.post("/api/public/generatetoken")
        .content(jsonRequest)
        .accept(MediaType.ALL)
        .contentType(MediaType.APPLICATION_JSON)
    )
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

  protected fun <C : Collection<E>?, E> mapResponse(
    result: MvcResult,
    collectionType: Class<C>?,
    elementType: Class<E>?
  ): C {
    return try {
      mapper.readValue(
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
