package io.tolgee.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.request.AbstractRepositoryDTO
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.fixtures.LoggedRequestFactory.loggedDelete
import io.tolgee.fixtures.LoggedRequestFactory.loggedGet
import io.tolgee.fixtures.LoggedRequestFactory.loggedPost
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.helpers.JsonHelper
import io.tolgee.model.Repository
import org.assertj.core.api.Assertions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.Rollback
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class RepositoryControllerTest : SignedInControllerTest() {
    private val languageDTO = LanguageDTO(null, "English", "en")

    @Test
    fun createRepository() {
        dbPopulator.createBase("test")
        testCreateValidationSize()
        testCreateValidationUniqueness()
        testCreateCorrectRequest()
    }

    private fun testCreateCorrectRequest() {
        val request = CreateRepositoryDTO("aaa", setOf(languageDTO))
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val aa: Optional<Repository> = repositoryService.findByName("aaa", userAccount)
        Assertions.assertThat(aa).isPresent
        val repository = aa.orElse(null)
        Assertions.assertThat(repository!!.languages).isNotEmpty
        val language = repository.languages.stream().findFirst().orElse(null)
        Assertions.assertThat(language).isNotNull
        Assertions.assertThat(language.abbreviation).isEqualTo("en")
        Assertions.assertThat(language.name).isEqualTo("English")
    }

    private fun testCreateValidationSize() {
        val request = CreateRepositoryDTO("aa", setOf(languageDTO))

        //test validation
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString).contains("name")
        Assertions.assertThat(mvcResult.response.contentAsString).contains("STANDARD_VALIDATION")
    }

    private fun testCreateValidationUniqueness() {
        val request = CreateRepositoryDTO("test", setOf(languageDTO))

        //test validation
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString)
                .isEqualTo("{\"STANDARD_VALIDATION\":{\"name\":\"NAME_EXISTS\"}}")
    }

    @Test
    fun editRepository() {
        val test = dbPopulator.createBase(generateUniqueString())
        val (name) = dbPopulator.createBase(generateUniqueString())
        testEditValidationUniqueness(test, name)
        testEditCorrectRequest(test)
    }

    private fun testEditCorrectRequest(test: Repository) {
        val request: AbstractRepositoryDTO = EditRepositoryDTO(test.id, "new test")
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val mapper = ObjectMapper()
        val response = mapper.readValue(mvcResult.response.contentAsString, RepositoryDTO::class.java)
        Assertions.assertThat(response.name).isEqualTo("new test")
        Assertions.assertThat(response.id).isEqualTo(test.id)
        val found = repositoryService.findByName("new test", userAccount)
        Assertions.assertThat(found).isPresent
    }

    private fun testEditValidationUniqueness(repository: Repository, nonUniqueName: String?) {
        val request = EditRepositoryDTO(repository.id, nonUniqueName)

        //test validation
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString)
                .isEqualTo("{\"STANDARD_VALIDATION\":{\"name\":\"NAME_EXISTS\"}}")
    }

    @Test
    @Rollback
    fun deleteRepository() {
        val base = dbPopulator.createBase(generateUniqueString())
        mvc.perform(
                loggedDelete("/api/repositories/${base.id}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val repository = repositoryService.getById(base.id)
        Assertions.assertThat(repository).isEmpty
    }

    @Test
    fun findAll() {
        val repos = LinkedHashSet<String?>()
        for (i in 0..2) {
            repos.add(dbPopulator.createBase(generateUniqueString()).name)
        }
        val mvcResult = mvc.perform(
                loggedGet("/api/repositories/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val set: Set<RepositoryDTO> = mvcResult.mapResponseTo()
        Assertions.assertThat(set).extracting("name").containsAll(repos)
    }
}
