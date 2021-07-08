package io.tolgee.fixtures

import org.springframework.context.annotation.Scope
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Component
@Scope("prototype")
class SignedInRequestPerformer : BaseRequestPerformer(), AuthRequestPerformer {

  override fun performAuthPut(url: String, content: Any?): ResultActions {
    return mvc.perform(LoggedRequestFactory.loggedPut(url).withJsonContent(content))
  }

  override fun performAuthPost(url: String, content: Any?): ResultActions {
    return mvc.perform(LoggedRequestFactory.loggedPost(url).withJsonContent(content))
  }

  override fun performAuthGet(url: String): ResultActions {
    return mvc.perform(LoggedRequestFactory.loggedGet(url))
  }

  override fun performAuthDelete(url: String, content: Any?): ResultActions {
    return mvc.perform(LoggedRequestFactory.loggedDelete(url).withJsonContent(content))
  }

  override fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>
  ): ResultActions {
    val builder = MockMvcRequestBuilders.multipart(url)
    files.forEach { builder.file(it) }
    params.forEach { (name, values) -> builder.param(name, *values) }

    return mvc.perform(LoggedRequestFactory.addToken(builder))
  }
}
