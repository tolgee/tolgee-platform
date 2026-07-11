package io.tolgee.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.fixtures.AuthorizedRequestFactory
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

fun performImport(
  mvc: MockMvc,
  projectId: Long,
  files: List<Pair<String, Resource>>?,
  params: Map<String, Any?> = mapOf(),
): ResultActions {
  val builder =
    MockMvcRequestBuilders
      .multipart("/v2/projects/$projectId/import?${mapToQueryString(params)}")

  files?.forEach {
    builder.file(
      MockMultipartFile(
        "files",
        it.first,
        "application/zip",
        it.second.file.readBytes(),
      ),
    )
  }

  return mvc.perform(AuthorizedRequestFactory.addToken(builder))
}

fun performSingleStepImport(
  mvc: MockMvc,
  projectId: Long,
  files: List<Pair<String, Resource>>?,
  params: Map<String, Any?> = mapOf(),
): ResultActions {
  val builder =
    MockMvcRequestBuilders
      .multipart("/v2/projects/$projectId/single-step-import")

  files?.forEach {
    builder.file(
      MockMultipartFile(
        "files",
        it.first,
        "application/zip",
        it.second.file.readBytes(),
      ),
    )
  }

  builder.part(MockPart("params", jacksonObjectMapper().writeValueAsBytes(params)))

  return mvc.perform(AuthorizedRequestFactory.addToken(builder))
}

private fun mapToQueryString(map: Map<String, Any?>): String {
  return map.entries.joinToString("&") { "${it.key}=${it.value}" }
}
