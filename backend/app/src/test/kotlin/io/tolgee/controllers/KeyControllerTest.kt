package io.tolgee.controllers

import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.key.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.key.OldEditKeyDto
import io.tolgee.dtos.request.translation.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Project
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KeyControllerTest : AuthorizedControllerTest() {
  private val keyDto = SetTranslationsWithKeyDto("test string", mapOf(Pair("en", "Hello")))
  private val keyDto2 = SetTranslationsWithKeyDto("test string 2", mapOf(Pair("en", "Hello 2")))

  private lateinit var project: Project

  @BeforeEach
  fun setup() {
    this.project = dbPopulator.createBase(generateUniqueString())
  }

  @Test
  fun create() {
    performCreate(projectId = project.id, content = keyDto).andExpect(status().`is`(200))
      .andReturn()

    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath("test string"))).isNotEmpty
  }

  @Test
  fun createValidation() {
    val result = performCreate(
      projectId = project.id,
      content = SetTranslationsWithKeyDto("", mapOf(Pair("en", "aaa")))
    )
      .andExpect(status().isBadRequest)
      .andReturn()
    assertThat(result).error().isStandardValidation
  }

  @Test
  fun editDeprecated() {
    keyService.create(project, keyDto)

    performAuthPost(
      "/api/project/${project.id}/keys/edit",
      DeprecatedEditKeyDTO(
        oldFullPathString = "test string",
        newFullPathString = "hello"
      )
    ).andExpect(status().`is`(200))
      .andReturn()

    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath("test string"))).isEmpty
    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath("hello"))).isNotEmpty
  }

  @Test
  fun edit() {
    keyService.create(project, keyDto)

    performEdit(
      projectId = project.id,
      content = OldEditKeyDto(
        currentName = "test string",
        newName = "hello"
      )
    )
      .andExpect(status().`is`(200))
      .andReturn()

    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath("test string"))).isEmpty
    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath("hello"))).isNotEmpty
  }

  @Test
  fun delete() {
    keyService.create(project, keyDto)
    keyService.create(project, keyDto2)

    val keyInstance = keyService.get(project.id, keyDto.key)

    performDelete(projectId = project.id, keyInstance.id)

    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath(keyDto.key))).isEmpty
    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath(keyDto2.key))).isNotEmpty
  }

  @Test
  fun deleteMultiple() {
    keyService.create(project, keyDto)
    keyService.create(project, keyDto2)

    val keyInstance = keyService.get(project.id, keyDto.key)
    val keyInstance2 = keyService.get(project.id, keyDto2.key)

    performDelete(projectId = project.id, setOf(keyInstance.id, keyInstance2.id))

    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath(keyDto.key))).isEmpty
    assertThat(keyService.findOptional(project.id, PathDTO.fromFullPath(keyDto2.key))).isEmpty
  }

  @Test
  fun get() {
    val key = keyService.create(project, keyDto)
    val got = performGet(key.project.id, key.id)
      .andExpect(status().isOk)
      .andReturn()
      .mapResponseTo<DeprecatedKeyDto>()
    assertThat(got.fullPathString).isEqualTo(key.path.fullPathString)
  }

  @Test
  fun getKeyTranslations() {
    val base = dbPopulator.populate(generateUniqueString())
    val got = performAuthPost(
      "/api/project/${base.id}/keys/translations/en,de",
      GetKeyTranslationsReqDto("sampleApp.hello_world!")
    ).andReturn()
      .mapResponseTo<Map<String, String>>()
    assertThat(got["en"]).isEqualTo("Hello world!")
    assertThat(got["de"]).isEqualTo("Hallo Welt!")
  }

  private fun performCreate(projectId: Long, content: SetTranslationsWithKeyDto): ResultActions {
    return performAuthPost("/api/project/$projectId/keys", content)
  }

  private fun performEdit(projectId: Long, content: OldEditKeyDto): ResultActions {
    return performAuthPut("/api/project/$projectId/keys", content)
  }

  private fun performDelete(projectId: Long, ids: Set<Long>): ResultActions {
    return performAuthDelete("/api/project/$projectId/keys", ids)
  }

  private fun performDelete(projectId: Long, id: Long): ResultActions {
    return performAuthDelete("/api/project/$projectId/keys/$id", null)
  }

  private fun performGet(projectId: Long, id: Long): ResultActions {
    return performAuthGet("/api/project/$projectId/keys/$id")
  }
}
