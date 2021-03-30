package io.tolgee.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.fixtures.LoggedRequestFactory.loggedDelete
import io.tolgee.fixtures.LoggedRequestFactory.loggedGet
import io.tolgee.fixtures.LoggedRequestFactory.loggedPost
import io.tolgee.fixtures.andIsOk
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
        testCreateCorrectRequest()
    }

    @Test
    fun createRepositoryOrganization() {
        val userAccount = dbPopulator.createUser("testuser")
        val organization = dbPopulator.createOrganization("Test Organization", userAccount)
        logAsUser("testuser", initialPasswordManager.initialPassword)
        val request = CreateRepositoryDTO("aaa", setOf(languageDTO), organizationId = organization.id)
        val result = performAuthPost("/api/repositories", request).andIsOk.andReturn().mapResponseTo<RepositoryDTO>()
        repositoryService.get(result.id!!).get().let {
            assertThat(it.organizationOwner?.id).isEqualTo(organization.id)
        }
    }


    private fun testCreateCorrectRequest() {
        val request = CreateRepositoryDTO("aaa", setOf(languageDTO))
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val repositoryDto = repositoryService.findAllPermitted(userAccount!!).find { it.name == "aaa" }
        Assertions.assertThat(repositoryDto).isNotNull
        val repository = repositoryService.get(repositoryDto!!.id!!).get()
        Assertions.assertThat(repository.languages).isNotEmpty
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

    @Test
    fun editRepository() {
        val test = dbPopulator.createBase(generateUniqueString())
        testEditCorrectRequest(test)
    }

    private fun testEditCorrectRequest(test: Repository) {
        val request = EditRepositoryDTO(test.id, "new test")
        val mvcResult = mvc.perform(
                loggedPost("/api/repositories/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val mapper = ObjectMapper()
        val response = mapper.readValue(mvcResult.response.contentAsString, RepositoryDTO::class.java)
        Assertions.assertThat(response.name).isEqualTo("new test")
        Assertions.assertThat(response.id).isEqualTo(test.id)
        val found = repositoryService.findAllPermitted(userAccount!!).find { it.name == "new test" }
        Assertions.assertThat(found).isNotNull
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
        val repository = repositoryService.get(base.id)
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
