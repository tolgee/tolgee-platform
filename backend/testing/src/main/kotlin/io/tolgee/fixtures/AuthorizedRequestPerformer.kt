package io.tolgee.fixtures

import org.springframework.context.annotation.Scope
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@Component
@Scope("prototype")
class AuthorizedRequestPerformer :
  BaseRequestPerformer(),
  AuthRequestPerformer {
  override fun performAuthPut(
    url: String,
    content: Any?,
  ): ResultActions {
    return mvc.perform(AuthorizedRequestFactory.loggedPut(url).withJsonContent(content))
  }

  override fun performAuthPost(
    url: String,
    content: Any?,
  ): ResultActions {
    return mvc.perform(AuthorizedRequestFactory.loggedPost(url).withJsonContent(content))
  }

  override fun performAuthGet(url: String): ResultActions {
    return mvc.perform(AuthorizedRequestFactory.loggedGet(url))
  }

  override fun performAuthDelete(
    url: String,
    content: Any?,
  ): ResultActions {
    return mvc.perform(AuthorizedRequestFactory.loggedDelete(url).withJsonContent(content))
  }

  override fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>>,
  ): ResultActions {
    val builder = MockMvcRequestBuilders.multipart(url)
    files.forEach { builder.file(it) }
    params.forEach { (name, values) -> builder.param(name, *values) }

    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }
}
