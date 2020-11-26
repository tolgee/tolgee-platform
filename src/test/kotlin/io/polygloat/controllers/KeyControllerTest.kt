package io.polygloat.controllers

import io.polygloat.dtos.request.EditKeyDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.helpers.JsonHelper
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.polygloat.Assertions.Assertions.assertThat
import io.polygloat.dtos.PathDTO
import io.polygloat.model.Repository
import org.testng.annotations.BeforeMethod

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerTest : SignedInControllerTest(), ITest {
    private val keyDto = SetTranslationsDTO("test string", mapOf(Pair("en", "Hello")))
    private val keyDto2 = SetTranslationsDTO("test string 2", mapOf(Pair("en", "Hello 2")))

    private lateinit var repository: Repository;

    @BeforeMethod
    fun setup() {
        this.repository = dbPopulator.createBase(generateUniqueString());
    }

    @Test
    fun create() {
        performCreate(repositoryId = repository.id, content = keyDto).andExpect(status().`is`(200))
                .andReturn();

        assertThat(keyService.get(repository, PathDTO.fromFullPath("test string"))).isNotEmpty;
    }

    @Test
    fun createValidation() {
        val result = performCreate(
                repositoryId = repository.id,
                content = SetTranslationsDTO("", mapOf(Pair("en", "aaa"))))
                .andExpect(status().`is`(400))
                .andReturn();
        assertThat(result).error().isStandardValidation;
    }

    @Test
    fun edit() {
        keyService.create(repository, keyDto);

        performEdit(
                repositoryId = repository.id,
                content = EditKeyDTO(
                        oldFullPathString = "test string",
                        newFullPathString = "hello"
                ))
                .andExpect(status().`is`(200))
                .andReturn();

        assertThat(keyService.get(repository, PathDTO.fromFullPath("test string"))).isEmpty;
        assertThat(keyService.get(repository, PathDTO.fromFullPath("hello"))).isNotEmpty;
    }

    @Test
    fun delete() {
        keyService.create(repository, keyDto)
        keyService.create(repository, keyDto2)

        val keyInstance = keyService.get(repository, PathDTO.fromFullPath(keyDto.key)).orElseGet(null);

        performDelete(repositoryId = repository.id, keyInstance.id!!)

        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto.key))).isEmpty;
        assertThat(keyService.get(repository, PathDTO.fromFullPath(keyDto2.key))).isNotEmpty;
    }

    private fun performCreate(repositoryId: Long, content: SetTranslationsDTO): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedPost("/api/repository/$repositoryId/keys")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(content)))
    }

    private fun performEdit(repositoryId: Long, content: EditKeyDTO): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedPost("/api/repository/$repositoryId/keys/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.asJsonString(content)))
    }

    private fun performDelete(repositoryId: Long, ids: Set<Int>): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedDelete("/api/repository/$repositoryId/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.asJsonString(ids)))
    }

    private fun performDelete(repositoryId: Long, id: Long): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedDelete("/api/repository/$repositoryId/keys/$id")
                        .contentType(MediaType.APPLICATION_JSON))
    }

    private fun performGet(repositoryId: Long, id: Long): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedGet("/api/repository/$repositoryId/keys/$id")
                        .contentType(MediaType.APPLICATION_JSON))
    }
}