package io.tolgee.ee.api.v2.controllers

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.dtos.response.ApiKeyDTO
import io.tolgee.ee.development.AiPromptCustomizationTestData
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.fixtures.node
import io.tolgee.model.Pat
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions

class AiPromptCustomizationControllerApiAccessTest : AbstractControllerTest() {
  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var testData: AiPromptCustomizationTestData
  private lateinit var tokens: OAuth2TestTokens

  @BeforeEach
  fun setup() {
    testData = AiPromptCustomizationTestData()
    testDataService.saveTestData(testData.root)
    tokens = OAuth2TestTokens(grantRepository, userAccountService, keyGenerator)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `personal access token sets language customization`() {
    putLanguage(patHeaders(testData.ownerPat), testData.project, testData.french.id).andIsOk.andAssertThatJson {
      node("description").isEqualTo(NEW_DESCRIPTION)
    }

    getLanguages(patHeaders(testData.ownerPat), testData.project).andIsOk.andAssertThatJson {
      node("_embedded.promptCustomizations[1].description").isEqualTo(NEW_DESCRIPTION)
    }
  }

  @Test
  fun `personal access token reads project customization`() {
    getProject(patHeaders(testData.ownerPat), testData.project).andIsOk.andAssertThatJson {
      node("description").isEqualTo("Project description")
    }
  }

  @Test
  fun `personal access token sets project customization`() {
    putProject(patHeaders(testData.ownerPat), testData.project).andIsOk.andAssertThatJson {
      node("description").isEqualTo(NEW_DESCRIPTION)
    }
    assertProjectDescription(testData.project, NEW_DESCRIPTION)
  }

  @Test
  fun `non-owner member with project permissions sets customizations with personal access token`() {
    putProject(patHeaders(testData.memberPat), testData.project).andIsOk
    putLanguage(patHeaders(testData.memberPat), testData.project, testData.czech.id).andIsOk
  }

  @Test
  fun `project API key with edit scopes uses all four operations`() {
    val headers = pakHeaders(testData.user, testData.project, Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT)

    getProject(headers, testData.project).andIsOk
    putProject(headers, testData.project).andIsOk
    getLanguages(headers, testData.project).andIsOk
    putLanguage(headers, testData.project, testData.czech.id).andIsOk
  }

  @Test
  fun `project API key without project edit cannot set project customization`() {
    val headers = pakHeaders(testData.user, testData.project, Scope.TRANSLATIONS_VIEW)

    putProject(headers, testData.project).andIsForbidden.andHasErrorMessage(Message.OPERATION_NOT_PERMITTED)
    assertProjectDescription(testData.project, "Project description")
  }

  @Test
  fun `project API key with project edit only cannot set language customization`() {
    val headers = pakHeaders(testData.user, testData.project, Scope.PROJECT_EDIT)

    putLanguage(headers, testData.project, testData.czech.id)
      .andIsForbidden
      .andHasErrorMessage(Message.OPERATION_NOT_PERMITTED)
  }

  @Test
  fun `project API key with any scope reads project customization`() {
    val headers = pakHeaders(testData.user, testData.project, Scope.TRANSLATIONS_VIEW)

    getProject(headers, testData.project).andIsOk.andAssertThatJson {
      node("description").isEqualTo("Project description")
    }
  }

  @Test
  fun `project API key is refused on another project`() {
    val headers = pakHeaders(testData.user, testData.project, Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT)
    val otherProject = testData.otherProjectBuilder.self

    getProject(headers, otherProject).andIsForbidden.andHasErrorMessage(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
    putProject(headers, otherProject).andIsForbidden.andHasErrorMessage(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
    getLanguages(headers, otherProject).andIsForbidden.andHasErrorMessage(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
    putLanguage(headers, otherProject, testData.otherProjectGerman.id)
      .andIsForbidden
      .andHasErrorMessage(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)

    assertProjectDescription(otherProject, "Other project description")
    assertLanguageDescription(testData.otherProjectGerman.id, "Other project German description")
  }

  @Test
  fun `user without project access is refused`() {
    val headers = patHeaders(testData.outsiderPat)

    getProject(headers, testData.project).andIsNotFound.andHasErrorMessage(Message.PROJECT_NOT_FOUND)
    putProject(headers, testData.project).andIsNotFound.andHasErrorMessage(Message.PROJECT_NOT_FOUND)
    getLanguages(headers, testData.project).andIsNotFound.andHasErrorMessage(Message.PROJECT_NOT_FOUND)
    putLanguage(headers, testData.project, testData.czech.id)
      .andIsNotFound
      .andHasErrorMessage(Message.PROJECT_NOT_FOUND)
  }

  @Test
  fun `OAuth access token sets project customization`() {
    val token =
      tokens.issue(
        subject = testData.user.id,
        scopes = listOf(Scope.PROJECT_EDIT.value, Scope.LANGUAGES_EDIT.value),
        projectIds = null,
      )

    putProject(bearerHeaders(token), testData.project).andIsOk.andAssertThatJson {
      node("description").isEqualTo(NEW_DESCRIPTION)
    }
  }

  @Test
  fun `language from another project is not found`() {
    putLanguage(patHeaders(testData.ownerPat), testData.project, testData.otherProjectGerman.id)
      .andIsNotFound
      .andHasErrorMessage(Message.LANGUAGE_NOT_FOUND)

    assertLanguageDescription(testData.otherProjectGerman.id, "Other project German description")
  }

  @Test
  fun `project API key loses access when its owner loses permission`() {
    val headers = pakHeaders(testData.member, testData.project, Scope.PROJECT_EDIT, Scope.TRANSLATIONS_VIEW)
    putProject(headers, testData.project).andIsOk

    executeInNewTransaction {
      permissionService.setUserDirectPermission(
        testData.project.id,
        testData.member.id,
        ProjectPermissionType.VIEW,
        LanguagePermissions(),
      )
    }

    getProject(headers, testData.project).andIsOk
    putProject(headers, testData.project).andIsForbidden.andHasErrorMessage(Message.OPERATION_NOT_PERMITTED)
  }

  private fun getProject(
    headers: HttpHeaders,
    project: Project,
  ): ResultActions = performGet("/v2/projects/${project.id}/ai-prompt-customization", headers)

  private fun putProject(
    headers: HttpHeaders,
    project: Project,
  ): ResultActions =
    performPut(
      "/v2/projects/${project.id}/ai-prompt-customization",
      mapOf("description" to NEW_DESCRIPTION),
      headers,
    )

  private fun getLanguages(
    headers: HttpHeaders,
    project: Project,
  ): ResultActions = performGet("/v2/projects/${project.id}/language-ai-prompt-customizations", headers)

  private fun putLanguage(
    headers: HttpHeaders,
    project: Project,
    languageId: Long,
  ): ResultActions =
    performPut(
      "/v2/projects/${project.id}/languages/$languageId/ai-prompt-customization",
      mapOf("description" to NEW_DESCRIPTION),
      headers,
    )

  private fun patHeaders(pat: Pat): HttpHeaders = HttpHeaders().apply { add(API_KEY_HEADER_NAME, pat.tokenWithPrefix) }

  private fun pakHeaders(
    user: UserAccount,
    project: Project,
    vararg scopes: Scope,
  ): HttpHeaders {
    val apiKey = ApiKeyDTO.fromEntity(apiKeyService.create(user, scopes.toSet(), project))
    return HttpHeaders().apply { add(API_KEY_HEADER_NAME, apiKey.key) }
  }

  private fun assertProjectDescription(
    project: Project,
    expected: String,
  ) {
    assertThat(projectService.get(project.id).aiTranslatorPromptDescription).isEqualTo(expected)
  }

  private fun assertLanguageDescription(
    languageId: Long,
    expected: String,
  ) {
    assertThat(languageService.findEntity(languageId)?.aiTranslatorPromptDescription).isEqualTo(expected)
  }

  companion object {
    private const val NEW_DESCRIPTION = "new description"
  }
}
