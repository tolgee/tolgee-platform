package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.PropertyModification
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
class V2LanguageControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private val languageDTO = LanguageRequest("en", "en", "en")
  private val languageDTOBlank = LanguageRequest("", "")
  private val languageDTOCorrect = LanguageRequest("Spanish", "Espanol", "es")

  @Test
  fun createLanguage() {
    val base = dbPopulator.createBase()
    val project = base.project
    createLanguageTestValidation(project.id)
    createLanguageCorrectRequest(project.id)
  }

  @Test
  fun editLanguage() {
    val base = dbPopulator.createBase()
    val project = base.project
    val en = project.findLanguageOptional("en").orElseThrow { NotFoundException() }
    val languageDTO =
      LanguageRequest(
        name = "newEnglish",
        tag = "newEn",
        originalName = "newOriginalEnglish",
        flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7",
      )
    performEdit(project.id, en.id, languageDTO).andIsOk.andAssertThatJson {
      node("name").isEqualTo(languageDTO.name)
      node("originalName").isEqualTo(languageDTO.originalName)
      node("tag").isEqualTo(languageDTO.tag)
      node("flagEmoji").isEqualTo(languageDTO.flagEmoji)
    }
    val dbLanguage = languageService.findByTag(languageDTO.tag, project.id)
    Assertions.assertThat(dbLanguage).isNotNull
    Assertions.assertThat(dbLanguage!!.name).isEqualTo(languageDTO.name)
    Assertions.assertThat(dbLanguage.originalName).isEqualTo(languageDTO.originalName)
    Assertions.assertThat(dbLanguage.flagEmoji).isEqualTo(languageDTO.flagEmoji)
  }

  @Test
  fun findAllLanguages() {
    val project = dbPopulator.createBase(generateUniqueString(), "ben", "pwd").project
    loginAsUser("ben")
    performFindAll(project.id).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.languages") {
        isArray.hasSize(2)
      }
    }
  }

  @Test
  fun deleteLanguage() {
    val base = dbPopulator.createBase()
    val project = base.project
    val deutsch = project.findLanguageOptional("de").orElseThrow { NotFoundException() }
    performDelete(project.id, deutsch.id).andExpect(MockMvcResultMatchers.status().isOk)
    executeInNewTransaction {
      Assertions.assertThat(languageService.findEntity(deutsch.id)).isNull()
      assertDeleteActivityCreated()
    }
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.LANGUAGES_EDIT])
  fun `deletes language with API key`() {
    val base = dbPopulator.createBase()
    this.userAccount = base.userAccount
    this.projectSupplier = { base.project }
    val deutsch = project.findLanguageOptional("de").orElseThrow { NotFoundException() }

    performProjectAuthDelete("languages/${deutsch.id}", null)
      .andExpect(MockMvcResultMatchers.status().isOk)
    executeInNewTransaction {
      Assertions.assertThat(languageService.findEntity(deutsch.id)).isNull()
    }
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `does not delete language with API key (permissions)`() {
    val base = dbPopulator.createBase()
    this.userAccount = base.userAccount
    this.projectSupplier = { base.project }
    val deutsch = project.findLanguageOptional("de").orElseThrow { NotFoundException() }
    executeInNewTransaction {
      performProjectAuthDelete("languages/${deutsch.id}", null).andIsForbidden
    }
  }

  @Test
  fun `cannot delete base language`() {
    val base = dbPopulator.createBase()
    executeInNewTransaction {
      val project = projectService.get(base.project.id)
      val en = project.findLanguageOptional("en").orElseThrow { NotFoundException() }
      project.baseLanguage = en
      projectService.save(project)

      performDelete(project.id, en.id).andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("cannot_delete_base_language")
      }
    }
  }

  @Test
  fun `automatically sets base language`() {
    val base = dbPopulator.createBase()

    val project =
      executeInNewTransaction {
        val project = projectService.get(base.project.id)
        project.baseLanguage = null
        projectService.save(project)
      }

    val en =
      executeInNewTransaction {
        // base language is set on load, so this should set new language
        val projectLocal = projectService.get(base.project.id)
        projectLocal.findLanguageOptional("en").orElseThrow { NotFoundException() }
      }

    performDelete(project.id, en.id).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("cannot_delete_base_language")
    }
  }

  @Test
  fun createLanguageTestValidationComa() {
    val base = dbPopulator.createBase()
    val project = base.project
    performCreate(
      project.id,
      LanguageRequest(originalName = "Original name", name = "Name", tag = "aa,aa"),
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
    Assertions.assertThat(es!!.name).isEqualTo(languageDTOCorrect.name)
  }

  fun createLanguageTestValidation(repoId: Long) {
    val mvcResult =
      performCreate(repoId, languageDTO)
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
        .andReturn()
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

  private fun performCreate(
    projectId: Long,
    content: LanguageRequest,
  ): ResultActions {
    return performAuthPost("/v2/projects/$projectId/languages", content)
  }

  private fun performEdit(
    projectId: Long,
    languageId: Long,
    content: LanguageRequest,
  ): ResultActions {
    return performAuthPut("/v2/projects/$projectId/languages/$languageId", content)
  }

  private fun performDelete(
    projectId: Long,
    languageId: Long,
  ): ResultActions {
    return performAuthDelete("/v2/projects/$projectId/languages/$languageId", null)
  }

  private fun performFindAll(projectId: Long): ResultActions {
    return performAuthGet("/v2/projects/$projectId/languages")
  }

  @Suppress("UNCHECKED_CAST")
  private fun assertDeleteActivityCreated() {
    val result =
      entityManager
        .createQuery(
          """select ar.id, ame.modifications, ame.describingData from ActivityRevision ar 
            |join ar.modifiedEntities ame
            |where ar.type = :type
          """.trimMargin(),
        ).setParameter("type", ActivityType.DELETE_LANGUAGE)
        .resultList as List<Array<Any>>
    val modifications = result[0][1] as Map<String, PropertyModification>
    modifications["deletedAt"]!!.old.assert.isNull()
  }
}
