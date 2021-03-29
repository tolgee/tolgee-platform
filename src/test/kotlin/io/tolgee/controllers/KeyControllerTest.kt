package io.tolgee.controllers

import io.tolgee.ITest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.EditKeyDTO
import io.tolgee.dtos.request.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Repository
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerTest : SignedInControllerTest(), ITest {
    private val keyDto = SetTranslationsDTO("test string", mapOf(Pair("en", "Hello")))
    private val keyDto2 = SetTranslationsDTO("test string 2", mapOf(Pair("en", "Hello 2")))

    private lateinit var repository: Repository

    @BeforeMethod
    fun setup() {
        this.repository = dbPopulator.createBase(generateUniqueString())
    }

    @Test
    fun create() {
        performCreate(repositoryId = repository.id, content = keyDto).andExpect(status().`is`(200))
                .andReturn()

        assertThat(keyService.get(repository, PathDTO.fromFullPath("test string"))).isNotEmpty
    }

    @Test
    fun createValidation() {
        val result = performCreate(
                repositoryId = repository.id,
                content = SetTranslationsDTO("", mapOf(Pair("en", "aaa"))))
                .andExpect(status().isBadRequest)
                .andReturn()
        assertThat(result).error().isStandardValidation
    }

    @Test
    fun editDeprecated() {
        keyService.create(repository, keyDto)

        performAuthPost("/api/repository/${repository.id}/keys/edit", DeprecatedEditKeyDTO(
                oldFullPathString = "test string",
                newFullPathString = "hello"
        )).andExpect(status().`is`(200))
        .andReturn()

        assertThat(keyService.get(repository, PathDTO.fromFullPath("test string"))).isEmpty
        assertThat(keyService.get(repository, PathDTO.fromFullPath("hello"))).isNotEmpty
    }

    @Test
    fun edit() {
        keyService.create(repository, keyDto)

        performEdit(
                repositoryId = repository.id,
                content = EditKeyDTO(
                        currentName = "test string",
                        newName = "hello"
                ))
                .andExpect(status().`is`(200))
                .andReturn()

        assertThat(keyService.get(repository, PathDTO.fromFullPath("test string"))).isEmpty
        assertThat(keyService.get(repository, PathDTO.fromFullPath("hello"))).isNotEmpty
    }

    @Test
    fun delete() {
        keyService.create(repository, keyDto)
        keyService.create(repository, keyDto2)

        val keyInstance = keyService.get(repository, PathDTO.fromFullPath(keyDto.key)).orElseGet(null)

        performDelete(repositoryId = repository.id, keyInstance.id!!)

        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto.key))).isEmpty
        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto2.key))).isNotEmpty
    }

    @Test
    fun deleteMultiple() {
        keyService.create(repository, keyDto)
        keyService.create(repository, keyDto2)

        val keyInstance = keyService.get(repository, PathDTO.fromFullPath(keyDto.key)).orElseGet(null)
        val keyInstance2 = keyService.get(repository, PathDTO.fromFullPath(keyDto2.key)).orElseGet(null)

        performDelete(repositoryId = repository.id, setOf(keyInstance.id!!, keyInstance2.id!!))

        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto.key))).isEmpty
        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto2.key))).isEmpty
    }

    @Test
    fun get() {
        val key = keyService.create(repository, keyDto)
        val got = performGet(key.repository!!.id, key.id!!)
                .andExpect(status().isOk)
                .andReturn()
                .mapResponseTo<DeprecatedKeyDto>()
        assertThat(got.fullPathString).isEqualTo(key.path.fullPathString)
    }

    @Test
    fun getKeyTranslations() {
        val base = dbPopulator.populate(generateUniqueString())
        val got = performAuthPost("/api/repository/${base.id}/keys/translations/en,de",
                GetKeyTranslationsReqDto("sampleApp.hello_world!")).andReturn()
                .mapResponseTo<Map<String, String>>()
        assertThat(got["en"]).isEqualTo("Hello world!")
        assertThat(got["de"]).isEqualTo("Hallo Welt!")
    }

    private fun performCreate(repositoryId: Long, content: SetTranslationsDTO): ResultActions {
        return performAuthPost("/api/repository/$repositoryId/keys", content)
    }

    private fun performEdit(repositoryId: Long, content: EditKeyDTO): ResultActions {
        return performAuthPut("/api/repository/$repositoryId/keys", content)
    }

    private fun performDelete(repositoryId: Long, ids: Set<Long>): ResultActions {
        return performAuthDelete("/api/repository/$repositoryId/keys", ids)
    }

    private fun performDelete(repositoryId: Long, id: Long): ResultActions {
        return performAuthDelete("/api/repository/$repositoryId/keys/$id", null)
    }

    private fun performGet(repositoryId: Long, id: Long): ResultActions {
        return performAuthGet("/api/repository/$repositoryId/keys/$id")
    }
}
