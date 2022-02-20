package io.tolgee.api.v2.controllers

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
class V2LanguageControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private val languageDTO = LanguageDto("en", "en", "en")
  private val languageDTOBlank = LanguageDto("", "")
  private val languageDTOCorrect = LanguageDto("Spanish", "Espanol", "es")

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
    val languageDTO = LanguageDto(
      name = "newEnglish", tag = "newEn", originalName = "newOriginalEnglish",
      flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7"
    )
    performEdit(test.id, en.id, languageDTO).andIsOk.andAssertThatJson {
      node("name").isEqualTo(languageDTO.name)
      node("originalName").isEqualTo(languageDTO.originalName)
      node("tag").isEqualTo(languageDTO.tag)
      node("flagEmoji").isEqualTo(languageDTO.flagEmoji)
    }
    val dbLanguage = languageService.findByTag(languageDTO.tag, test.id)
    Assertions.assertThat(dbLanguage).isPresent
    Assertions.assertThat(dbLanguage.get().name).isEqualTo(languageDTO.name)
    Assertions.assertThat(dbLanguage.get().originalName).isEqualTo(languageDTO.originalName)
    Assertions.assertThat(dbLanguage.get().flagEmoji).isEqualTo(languageDTO.flagEmoji)
  }

  @Test
  fun findAllLanguages() {
    val project = dbPopulator.createBase(generateUniqueString(), "ben", "pwd")
    loginAsUser("ben")
    performFindAll(project.id).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.languages") {
        isArray.hasSize(2)
      }
    }
  }

  @Test
  fun deleteLanguage() {
    val test = dbPopulator.createBase(generateUniqueString())
    val deutsch = test.getLanguage("de").orElseThrow { NotFoundException() }
    performDelete(test.id, deutsch.id).andExpect(MockMvcResultMatchers.status().isOk)
    Assertions.assertThat(languageService.findById(deutsch.id)).isEmpty
  }

  @Test
  fun `cannot delete base language`() {
    val test = dbPopulator.createBase(generateUniqueString())
    val en = test.getLanguage("en").orElseThrow { NotFoundException() }
    test.baseLanguage = en
    projectService.save(test)
    performDelete(test.id, en.id).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("cannot_delete_base_language")
    }
  }

  @Test
  fun `automatically sets base language`() {
    val test = dbPopulator.createBase(generateUniqueString())
    val en = test.getLanguage("en").orElseThrow { NotFoundException() }
    test.baseLanguage = null
    projectService.save(test)
    performDelete(test.id, en.id).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("cannot_delete_base_language")
    }
  }

  @Test
  fun createLanguageTestValidationComa() {
    val project = dbPopulator.createBase(generateUniqueString())
    performCreate(
      project.id,
      LanguageDto(originalName = "Original name", name = "Name", tag = "aa,aa")
    ).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION.tag").isEqualTo("can not contain coma")
    }
  }

  private fun createLanguageCorrectRequest(repoId: Long) {
    performCreate(repoId, languageDTOCorrect).andIsOk.andAssertThatJson {
      node("name").isEqualTo(languageDTOCorrect.name)
      node("tag").isEqualTo(languageDTOCorrect.tag)
    }
    val es = languageService.findByTag("es", repoId)
    Assertions.assertThat(es).isPresent
    Assertions.assertThat(es.get().name).isEqualTo(languageDTOCorrect.name)
  }

  fun createLanguageTestValidation(repoId: Long) {
    val mvcResult = performCreate(repoId, languageDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    Assertions.assertThat(mvcResult.response.contentAsString).contains("language_tag_exists")
    Assertions.assertThat(mvcResult.response.contentAsString).contains("language_name_exists")
    performCreate(repoId, languageDTOBlank).andIsBadRequest.andAssertThatJson {
      node("STANDARD_VALIDATION").apply {
        node("name").isEqualTo("must not be blank")
        node("tag").isEqualTo("must not be blank")
        node("originalName").isEqualTo("must not be blank")
      }
    }
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun findAllLanguagesApiKey() {
    performProjectAuthGet("languages").andIsOk.andAssertThatJson {
      node("_embedded.languages").isArray.hasSize(2)
    }
  }

  private fun performCreate(projectId: Long, content: LanguageDto): ResultActions {
    return performAuthPost("/v2/projects/$projectId/languages", content)
  }

  private fun performEdit(projectId: Long, languageId: Long, content: LanguageDto): ResultActions {
    return performAuthPut("/v2/projects/$projectId/languages/$languageId", content)
  }

  private fun performDelete(projectId: Long, languageId: Long): ResultActions {
    return performAuthDelete("/v2/projects/$projectId/languages/$languageId", null)
  }

  private fun performFindAll(projectId: Long): ResultActions {
    return performAuthGet("/v2/projects/$projectId/languages")
  }
}
