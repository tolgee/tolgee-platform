package io.tolgee.controllers

import io.tolgee.ITest
import io.tolgee.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.LoggedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.helpers.JsonHelper
import org.assertj.core.api.Assertions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class LanguageControllerTest : ProjectAuthControllerTest(), ITest {
    private val languageDTO = LanguageDTO(null, "en", "en")
    private val languageDTOBlank = LanguageDTO(null, "")
    private val languageDTOCorrect = LanguageDTO(null, "Espanol", "es")

    @Test
    fun createLanguage() {
        val project = dbPopulator.createBase(generateUniqueString())
        createLanguageTestValidation(project.id)
        createLanguageCorrectRequest(project.id)
    }

    @Test
    fun editLanguage() {
        val test = dbPopulator.createBase(generateUniqueString())
        val en = test.getLanguage("en").orElseThrow { NotFoundException() }
        val languageDTO = LanguageDTO.fromEntity(en)
        languageDTO.name = "newEnglish"
        languageDTO.abbreviation = "newEn"
        val mvcResult = performEdit(test.id, languageDTO)
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val languageDTORes = decodeJson(mvcResult.response.contentAsString, LanguageDTO::class.java)
        Assertions.assertThat(languageDTORes.name).isEqualTo(languageDTO.name)
        Assertions.assertThat(languageDTORes.abbreviation).isEqualTo(languageDTO.abbreviation)
        val dbLanguage = languageService.findByAbbreviation(languageDTO.abbreviation, test.id)
        Assertions.assertThat(dbLanguage).isPresent
        Assertions.assertThat(dbLanguage.get().name).isEqualTo(languageDTO.name)
    }

    @Test
    fun findAllLanguages() {
        val project = dbPopulator.createBase(generateUniqueString(), "ben", "pwd")
        logAsUser("ben", "pwd")
        val mvcResult = performFindAll(project.id).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        assertThat(decodeJson(mvcResult.response.contentAsString, Set::class.java)).hasSize(2)
    }

    @Test
    fun deleteLanguage() {
        val test = dbPopulator.createBase(generateUniqueString())
        val en = test.getLanguage("en").orElseThrow { NotFoundException() }
        performDelete(test.id, en.id!!).andExpect(MockMvcResultMatchers.status().isOk)
        Assertions.assertThat(languageService.findById(en.id!!)).isEmpty
        projectService.deleteRepository(test.id)
    }

    @Test
    fun createLanguageTestValidationComa() {
        val project = dbPopulator.createBase(generateUniqueString())
        val mvcResult = performCreate(project.id, LanguageDTO(name = "Name", abbreviation = "aa,aa"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString)
                .isEqualTo("{\"STANDARD_VALIDATION\":" +
                        "{\"abbreviation\":\"can not contain coma\"}}")
    }

    private fun createLanguageCorrectRequest(repoId: Long) {
        val mvcResult = performCreate(repoId, languageDTOCorrect).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val languageDTO = decodeJson(mvcResult.response.contentAsString, LanguageDTO::class.java)
        Assertions.assertThat(languageDTO.name).isEqualTo(languageDTOCorrect.name)
        Assertions.assertThat(languageDTO.abbreviation).isEqualTo(languageDTOCorrect.abbreviation)
        val es = languageService.findByAbbreviation("es", repoId)
        Assertions.assertThat(es).isPresent
        Assertions.assertThat(es.get().name).isEqualTo(languageDTOCorrect.name)
    }

    fun createLanguageTestValidation(repoId: Long) {
        var mvcResult = performCreate(repoId, languageDTO)
                .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString).contains("language_abbreviation_exists")
        Assertions.assertThat(mvcResult.response.contentAsString).contains("language_name_exists")
        mvcResult = performCreate(repoId, languageDTOBlank)
                .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        Assertions.assertThat(mvcResult.response.contentAsString)
                .isEqualTo("{\"STANDARD_VALIDATION\":" +
                        "{\"name\":\"must not be blank\"," +
                        "\"abbreviation\":\"must not be blank\"}}")
    }

    @Test
    @ProjectApiKeyAuthTestMethod
    fun findAllLanguagesApiKey() {
        val contentAsString = performRepositoryAuthGet("languages").andIsOk.andReturn().mapResponseTo<Set<Any>>()
        assertThat(contentAsString).hasSize(2)
    }

    private fun performCreate(projectId: Long, content: LanguageDTO): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedPost("/api/project/$projectId/languages")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(content)))
    }

    private fun performEdit(projectId: Long, content: LanguageDTO): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedPost("/api/project/$projectId/languages/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                                JsonHelper.asJsonString(content)))
    }

    private fun performDelete(projectId: Long, languageId: Long): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedDelete("/api/project/$projectId/languages/$languageId")
                        .contentType(MediaType.APPLICATION_JSON))
    }

    private fun performFindAll(projectId: Long): ResultActions {
        return mvc.perform(
                LoggedRequestFactory.loggedGet("/api/project/$projectId/languages")
                        .contentType(MediaType.APPLICATION_JSON))
    }
}
