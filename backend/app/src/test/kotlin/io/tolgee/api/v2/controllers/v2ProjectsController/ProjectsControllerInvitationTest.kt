package io.tolgee.api.v2.controllers.v2ProjectsController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.InvitationTestUtil
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration::class)
class ProjectsControllerInvitationTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    private const val INVITED_EMAIL = "jon@doe.com"
    private const val INVITED_NAME = "Franta"
  }

  @BeforeEach
  @AfterEach
  fun reset() {
    emailTestUtil.initMocks()
  }

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  val invitationTestUtil: InvitationTestUtil by lazy {
    InvitationTestUtil(this, applicationContext)
  }

  @Test
  fun `returns project invitations`() {
    val base = dbPopulator.createBase()
    val project = base.project
    createTranslateInvitation(project)
    performAuthGet("/v2/projects/${project.id}/invitations").andIsOk.andAssertThatJson {
      node("_embedded.invitations[0]") {
        node("type").isEqualTo("TRANSLATE")
        node("permittedLanguageIds").isArray.hasSize(2)
        node("invitedUserName").isEqualTo("Franta")
        node("invitedUserEmail").isEqualTo("a@a.a")
        node("permission") {
          node("type").isEqualTo("TRANSLATE")
          node("stateChangeLanguageIds").isArray.hasSize(0)
          node("translateLanguageIds").isArray.hasSize(2)
          node("viewLanguageIds").isArray.hasSize(0)
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `invites user to project with languages (translate)`() {
    val result =
      invitationTestUtil
        .perform { getLang ->
          type = ProjectPermissionType.TRANSLATE
          languages = setOf(getLang("en"))
        }.andIsOk
    executeInNewTransaction {
      val invitation = invitationTestUtil.getInvitation(result)
      invitation.permission
        ?.translateLanguages!!
        .map { it.tag }
        .assert
        .contains("en") // stores
      invitation.permission
        ?.viewLanguages!!
        .map { it.tag }
        .assert
        .contains() // ads also to view
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `invites user to project with languages (review)`() {
    val result =
      invitationTestUtil
        .perform { getLang ->
          type = ProjectPermissionType.REVIEW
          translateLanguages = setOf(getLang("en"))
          stateChangeLanguages = setOf(getLang("en"))
        }.andIsOk
    executeInNewTransaction {
      val invitation = invitationTestUtil.getInvitation(result)
      invitation.permission
        ?.stateChangeLanguages!!
        .map { it.tag }
        .assert
        .contains("en") // stores
      invitation.permission
        ?.viewLanguages!!
        .map { it.tag }
        .assert
        .contains() // ads also to view
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot set different languages (review)`() {
    invitationTestUtil
      .perform { getLang ->
        type = ProjectPermissionType.REVIEW
        translateLanguages = setOf(getLang("en"))
        stateChangeLanguages = setOf()
      }.andIsBadRequest
      .andHasErrorMessage(
        Message.CANNOT_SET_DIFFERENT_TRANSLATE_AND_STATE_CHANGE_LANGUAGES_FOR_LEVEL_BASED_PERMISSIONS,
      )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `validates languages and permission (lower)`() {
    invitationTestUtil
      .perform { getLang ->
        type = ProjectPermissionType.VIEW
        translateLanguages = setOf(getLang("en"))
      }.andIsBadRequest
      .andHasErrorMessage(Message.ONLY_TRANSLATE_OR_REVIEW_PERMISSION_ACCEPTS_TRANSLATE_LANGUAGES)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `validates languages and permission (higher)`() {
    invitationTestUtil
      .perform { getLang ->
        type = ProjectPermissionType.EDIT
        translateLanguages = setOf(getLang("en"))
      }.andIsBadRequest
      .andHasErrorMessage(Message.ONLY_TRANSLATE_OR_REVIEW_PERMISSION_ACCEPTS_TRANSLATE_LANGUAGES)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `code has 50 characters`() {
    val key = inviteWithManagePermissions()
    assertThat(key).hasSize(50)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stores name and e-mail with invitation`() {
    val code = inviteWithUserWithNameAndEmail()
    val invitation = invitationService.getInvitation(code)
    assertThat(invitation.name).isEqualTo(INVITED_NAME)
    assertThat(invitation.email).isEqualTo(INVITED_EMAIL)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends invitation e-mail`() {
    val code = inviteWithUserWithNameAndEmail()
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.verifyEmailSent()
    }

    val messageContent = emailTestUtil.messageContents.single()
    assertThat(messageContent).contains(code)
    assertThat(messageContent).contains("https://dummy-url.com")
    emailTestUtil.assertEmailTo.isEqualTo(INVITED_EMAIL)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `uses frontEnd url when possible`() {
    inviteWithUserWithNameAndEmail()
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.verifyEmailSent()
    }

    val messageContent = emailTestUtil.messageContents.single()
    assertThat(messageContent).contains("https://dummy-url.com")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not invite when email already invited`() {
    performInviteWithNameAndEmail().andIsOk
    performInviteWithNameAndEmail().andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not invite when email already member`() {
    transactionTemplate.execute {
      val user = dbPopulator.createUserIfNotExists("hello@hello.com")
      val user2 = dbPopulator.createUserIfNotExists("hello@hello2.com")
      val organization = dbPopulator.createOrganization("org", user)
      val project = dbPopulator.createProject("hello", organization)
      permissionService.create(
        Permission(
          user = user2,
          project = project,
          type = ProjectPermissionType.MANAGE,
        ),
      )
      userAccount = user
      projectSupplier = { project }
    }

    performAuthPut(
      "/v2/projects/${project.id}/invite",
      ProjectInviteUserDto(
        type = ProjectPermissionType.VIEW,
        email = "hello@hello2.com",
        name = "Franta",
      ),
    ).andIsBadRequest
  }

  private fun inviteWithManagePermissions(): String {
    val invitationJson =
      performProjectAuthPut("/invite", ProjectInviteUserDto(ProjectPermissionType.MANAGE))
        .andIsOk.andGetContentAsString
    return parseCode(invitationJson)
  }

  private fun inviteWithUserWithNameAndEmail(): String {
    val invitationJson = performInviteWithNameAndEmail().andIsOk.andGetContentAsString
    return parseCode(invitationJson)
  }

  private fun parseCode(invitationJson: String) =
    jacksonObjectMapper().readValue<Map<String, Any>>(invitationJson)["code"] as String

  private fun performInviteWithNameAndEmail() =
    performProjectAuthPut(
      "/invite",
      ProjectInviteUserDto(
        type = ProjectPermissionType.MANAGE,
        email = INVITED_EMAIL,
        name = INVITED_NAME,
      ),
    )

  private fun prepareTestData(): BaseTestData {
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    return testData
  }

  private fun createTranslateInvitation(project: Project): Invitation {
    return invitationService.create(
      CreateProjectInvitationParams(
        project = project,
        type = ProjectPermissionType.TRANSLATE,
        languagePermissions = LanguagePermissions(translate = project.languages),
        name = "Franta",
        email = "a@a.a",
        scopes = null,
      ),
    )
  }
}
