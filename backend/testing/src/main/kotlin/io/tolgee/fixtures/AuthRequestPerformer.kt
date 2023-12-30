package io.tolgee.fixtures

import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions

interface AuthRequestPerformer : RequestPerformer {
  fun performAuthPut(
    url: String,
    content: Any?,
  ): ResultActions

  fun performAuthPost(
    url: String,
    content: Any?,
  ): ResultActions

  fun performAuthGet(url: String): ResultActions

  fun performAuthDelete(
    url: String,
    content: Any? = null,
  ): ResultActions

  fun performAuthMultipart(
    url: String,
    files: List<MockMultipartFile>,
    params: Map<String, Array<String>> = mapOf(),
  ): ResultActions
}
