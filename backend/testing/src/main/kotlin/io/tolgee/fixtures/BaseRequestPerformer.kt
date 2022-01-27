package io.tolgee.fixtures

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Component
@Scope("prototype")
class BaseRequestPerformer : RequestPerformer {

  @field:Autowired
  lateinit var mvc: MockMvc

  override fun perform(builder: MockHttpServletRequestBuilder): ResultActions {
    return try {
      mvc.perform(builder)
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  override fun performPut(url: String, content: Any?): ResultActions {
    return perform(MockMvcRequestBuilders.put(url).withJsonContent(content))
  }

  override fun performPost(url: String, content: Any?): ResultActions {
    return mvc.perform(MockMvcRequestBuilders.post(url).withJsonContent(content))
  }

  override fun performGet(url: String): ResultActions {
    return perform(MockMvcRequestBuilders.get(url))
  }

  override fun performDelete(url: String, content: Any?): ResultActions {
    return perform(MockMvcRequestBuilders.delete(url).withJsonContent(content))
  }

  protected fun MockHttpServletRequestBuilder.withJsonContent(content: Any?): MockHttpServletRequestBuilder {
    return this.contentType(MediaType.APPLICATION_JSON).content(jacksonObjectMapper().writeValueAsString(content))
  }
}
