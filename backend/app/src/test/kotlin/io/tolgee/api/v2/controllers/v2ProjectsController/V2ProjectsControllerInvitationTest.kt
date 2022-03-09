package io.tolgee.api.v2.controllers.v2ProjectsController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.organization.OrganizationInviteUserDto
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.fixtures.JavaMailSenderMocked
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.node
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import javax.mail.internet.MimeMessage

@SpringBootTest
@AutoConfigureMockMvc
class V2ProjectsControllerInvitationTest : ProjectAuthControllerTest("/v2/projects/"), JavaMailSenderMocked {

  companion object {
    private const val INVITED_EMAIL = "jon@doe.com"
    private const val INVITED_NAME = "Franta"
  }

  @Autowired
  @MockBean
  override lateinit var javaMailSender: JavaMailSender

  override lateinit var messageArgumentCaptor: ArgumentCaptor<MimeMessage>

  @BeforeEach
  @AfterEach
  fun reset() {
    tolgeeProperties.frontEndUrl = null
  }

  @Test
  fun `returns project invitations`() {
    val project = dbPopulator.createBase(generateUniqueString())
    tolgeeProperties.frontEndUrl = "https://dummyUrl.com"
    createTranslateInvitation(project)
    performAuthGet("/v2/projects/${project.id}/invitations").andIsOk.andAssertThatJson {
      node("_embedded.invitations[0]") {
        node("type").isEqualTo("TRANSLATE")
        node("permittedLanguageIds").isArray.hasSize(2)
        node("invitedUserName").isEqualTo("Franta")
        node("invitedUserEmail").isEqualTo("a@a.a")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `invites user to project with languages`() {
    val testData = prepareTestData()

    val invitatationJson = performProjectAuthPut(
      "/invite",
      ProjectInviteUserDto(
        Permission.ProjectPermissionType.TRANSLATE, languages = setOf(testData.englishLanguage.id)
      )
    ).andIsOk.andGetContentAsString

    val key = parseCode(invitatationJson)

    val invitation = invitationService.getInvitation(key)
    assertThat(invitation.permission?.languages?.toList()?.first()?.tag).isEqualTo("en")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when provide languages for non TRANSLATE type`() {
    val testData = prepareTestData()

    performProjectAuthPut(
      "/invite",
      ProjectInviteUserDto(
        Permission.ProjectPermissionType.EDIT, languages = setOf(testData.englishLanguage.id)
      )
    ).andIsBadRequest
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
    verify(javaMailSender).send(messageArgumentCaptor.capture())

    val messageContent = messageArgumentCaptor.value.tolgeeStandardMessageContent
    assertThat(messageContent).contains(code)
    assertThat(messageContent).contains("http://localhost/")
    assertEmailTo().isEqualTo(INVITED_EMAIL)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `uses frontEnd url when possible`() {
    tolgeeProperties.frontEndUrl = "dummy_fe_url"
    inviteWithUserWithNameAndEmail()
    verify(javaMailSender).send(messageArgumentCaptor.capture())

    val messageContent = messageArgumentCaptor.value.tolgeeStandardMessageContent
    assertThat(messageContent).contains("dummy_fe_url")
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
    userAccount!!.username = "hello@hello.com"
    userAccountService.save(userAccount!!)

    performAuthPut(
      "/v2/projects/${project.id}/invite",
      OrganizationInviteUserDto(
        roleType = OrganizationRoleType.MEMBER,
        email = "hello@hello.com",
        name = "Franta"
      )
    ).andIsBadRequest
  }

  private fun inviteWithManagePermissions(): String? {
    val invitationJson = performProjectAuthPut("/invite", ProjectInviteUserDto(Permission.ProjectPermissionType.MANAGE))
      .andIsOk.andGetContentAsString
    return parseCode(invitationJson)
  }

  private fun inviteWithUserWithNameAndEmail(): String? {
    val invitationJson = performInviteWithNameAndEmail().andIsOk.andGetContentAsString
    return parseCode(invitationJson)
  }

  private fun parseCode(invitationJson: String) =
    jacksonObjectMapper().readValue<Map<String, Any>>(invitationJson)["code"] as String

  private fun performInviteWithNameAndEmail() = performProjectAuthPut(
    "/invite",
    ProjectInviteUserDto(
      type = Permission.ProjectPermissionType.MANAGE,
      email = INVITED_EMAIL,
      name = INVITED_NAME
    )
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
        type = Permission.ProjectPermissionType.TRANSLATE,
        languages = project.languages.toList(),
        name = "Franta",
        email = "a@a.a"
      )
    )
  }
}
