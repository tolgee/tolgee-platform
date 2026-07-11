package io.tolgee.testing

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.AbstractSpringTest
import io.tolgee.dtos.security.LoginRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.RequestPerformer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.io.UnsupportedEncodingException

@AutoConfigureMockMvc
@SpringBootTest
abstract class AbstractControllerTest :
  AbstractSpringTest(),
  RequestPerformer {
  @Autowired
  protected lateinit var mvc: MockMvc

  @Qualifier("baseRequestPerformer")
  @Autowired
  protected lateinit var requestPerformer: RequestPerformer

  fun <T> decodeJson(
    json: String?,
    clazz: Class<T>?,
  ): T {
    val mapper = jacksonObjectMapper()
    return try {
      mapper.readValue(json, clazz)
    } catch (e: JsonProcessingException) {
      throw RuntimeException(e)
    }
  }

  protected fun login(
    userName: String,
    password: String,
  ): DefaultAuthenticationResult {
    val response =
      doAuthentication(userName, password)
        .andReturn()
        .response.contentAsString
    val userAccount = userAccountService.findActive(userName) ?: throw NotFoundException()
    return DefaultAuthenticationResult(
      mapper.readValue(response, HashMap::class.java)["accessToken"] as String,
      userAccount,
    )
  }

  protected fun doAuthentication(
    username: String,
    password: String,
  ): ResultActions {
    val request = LoginRequest()
    request.username = username
    request.password = password
    val jsonRequest = mapper.writeValueAsString(request)
    return mvc.perform(
      MockMvcRequestBuilders
        .post("/api/public/generatetoken")
        .content(jsonRequest)
        .accept(MediaType.ALL)
        .contentType(MediaType.APPLICATION_JSON),
    )
  }

  protected fun <T> mapResponse(
    result: MvcResult,
    type: JavaType?,
  ): T {
    return try {
      mapper.readValue(result.response.contentAsString, type)
    } catch (e: JsonProcessingException) {
      throw RuntimeException(e)
    } catch (e: UnsupportedEncodingException) {
      throw RuntimeException(e)
    }
  }

  protected fun <T> mapResponse(
    result: MvcResult,
    clazz: Class<T>?,
  ): T {
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
    elementType: Class<E>?,
  ): C {
    return try {
      mapper.readValue(
        result.response.contentAsString,
        TypeFactory.defaultInstance().constructCollectionType(collectionType, elementType),
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

  override fun performPut(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performPut(url, content, httpHeaders)
  }

  override fun performPost(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performPost(url, content, httpHeaders)
  }

  override fun performGet(
    url: String,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performGet(url, httpHeaders)
  }

  override fun performDelete(
    url: String,
    content: Any?,
    httpHeaders: HttpHeaders,
  ): ResultActions {
    return requestPerformer.performDelete(url, content, httpHeaders)
  }
}
