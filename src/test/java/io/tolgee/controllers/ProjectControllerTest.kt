package io.tolgee.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.request.CreateProjectDTO
import io.tolgee.dtos.request.EditProjectDTO
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.dtos.response.ProjectDTO
import io.tolgee.fixtures.LoggedRequestFactory.loggedDelete
import io.tolgee.fixtures.LoggedRequestFactory.loggedGet
import io.tolgee.fixtures.LoggedRequestFactory.loggedPost
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.helpers.JsonHelper
import io.tolgee.model.Project
import org.assertj.core.api.Assertions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest : SignedInControllerTest() {
    private val languageDTO = LanguageDTO(null, "English", "en")

    @Test
    fun createProject() {
        dbPopulator.createBase("test")
        testCreateValidationSizeShort()
        testCreateValidationSizeLong()
        testCreateCorrectRequest()
    }

    @Test
    fun createProjectOrganization() {
        val userAccount = dbPopulator.createUserIfNotExists("testuser")
        val organization = dbPopulator.createOrganization("Test Organization", userAccount)
        logAsUser("testuser", initialPasswordManager.initialPassword)
        val request = CreateProjectDTO("aaa", setOf(languageDTO), organizationId = organization.id)
        val result = performAuthPost("/api/projects", request).andIsOk.andReturn().mapResponseTo<ProjectDTO>()
        projectService.get(result.id!!).get().let {
            assertThat(it.organizationOwner?.id).isEqualTo(organization.id)
        }
    }

    private fun testCreateCorrectRequest() {
        val request = CreateProjectDTO("aaa", setOf(languageDTO))
        val mvcResult = mvc.perform(
                loggedPost("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val projectDto = projectService.findAllPermitted(userAccount!!).find { it.name == "aaa" }
        Assertions.assertThat(projectDto).isNotNull
        val project = projectService.get(projectDto!!.id!!).get()
        Assertions.assertThat(project.languages).isNotEmpty
        val language = project.languages.stream().findFirst().orElse(null)
        Assertions.assertThat(language).isNotNull
        Assertions.assertThat(language.abbreviation).isEqualTo("en")
        Assertions.assertThat(language.name).isEqualTo("English")
    }

    private fun testCreateValidationSizeShort() {
        val request = CreateProjectDTO("aa", setOf(languageDTO))
        val mvcResult = performAuthPost("/api/projects", request).andIsBadRequest.andReturn()
        assertThat(mvcResult).error().isStandardValidation
    }

    private fun testCreateValidationSizeLong() {
        val request = CreateProjectDTO(
                "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...",
                setOf(languageDTO)
        )
        val mvcResult = performAuthPost("/api/projects", request).andIsBadRequest.andReturn()
        assertThat(mvcResult).error().isStandardValidation
    }

    @Test
    fun editProject() {
        val test = dbPopulator.createBase(generateUniqueString())
        testEditCorrectRequest(test)
    }

    private fun testEditCorrectRequest(test: Project) {
        val request = EditProjectDTO(test.id, "new test")
        val mvcResult = mvc.perform(
                loggedPost("/api/projects/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val mapper = ObjectMapper()
        val response = mapper.readValue(mvcResult.response.contentAsString, ProjectDTO::class.java)
        Assertions.assertThat(response.name).isEqualTo("new test")
        Assertions.assertThat(response.id).isEqualTo(test.id)
        val found = projectService.findAllPermitted(userAccount!!).find { it.name == "new test" }
        Assertions.assertThat(found).isNotNull
    }

    @Test
    fun deleteProject() {
        val base = dbPopulator.createBase(generateUniqueString())
        mvc.perform(
                loggedDelete("/api/projects/${base.id}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val project = projectService.get(base.id)
        Assertions.assertThat(project).isEmpty
    }

    @Test
    fun findAll() {
        val repos = LinkedHashSet<String?>()
        for (i in 0..2) {
            repos.add(dbPopulator.createBase(generateUniqueString()).name)
        }
        val mvcResult = mvc.perform(
                loggedGet("/api/projects/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val set: Set<ProjectDTO> = mvcResult.mapResponseTo()
        Assertions.assertThat(set).extracting("name").containsAll(repos)
    }
}
