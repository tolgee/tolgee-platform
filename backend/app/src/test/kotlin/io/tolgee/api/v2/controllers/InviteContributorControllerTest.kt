package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InviteContributorControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @BeforeEach
  fun setup() {
    emailTestUtil.initMocks()
    testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
    recordProjectActivity(testData.contributor.id, testData.project.id)
    recordProjectActivity(testData.contributor2.id, testData.publicProject.id)
    recordProjectActivity(testData.deletedContributor.id, testData.project.id)
    recordProjectActivity(testData.disabledContributor.id, testData.project.id)
    recordProjectActivity(testData.member.id, testData.project.id)
    recordProjectActivity(testData.orgMember.id, testData.project.id)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `invites a visible contributor, stores the email hidden, never reveals it`() {
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "TRANSLATE"),
    ).andIsOk
      .andAssertThatJson {
        node("invitedUserName").isEqualTo("Cora Contributor")
        node("invitedUserEmail").isNull()
      }

    emailTestUtil.verifyEmailSent()
    emailTestUtil.assertEmailTo.isEqualTo(testData.contributor.username)

    val invitation = invitationService.getForProject(testData.project).single()
    assertThat(invitation.emailHidden).isTrue()
    assertThat(invitation.email).isEqualTo(testData.contributor.username)
    executeInNewTransaction {
      val loaded = invitationService.getForProject(testData.project).single()
      assertThat(loaded.permission!!.type).isEqualTo(ProjectPermissionType.TRANSLATE)
    }

    performAuthGet("/api/public/invitation_info/${invitation.code}")
      .andIsOk
      .andAssertThatJson {
        node("inviteeEmail").isNull()
      }
  }

  @Test
  fun `requires MEMBERS_EDIT`() {
    userAccount = testData.member
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "TRANSLATE"),
    ).andIsForbidden
  }

  @Test
  fun `returns a uniform 404 for every id that is not a visible contributor`() {
    userAccount = testData.admin
    val bodies =
      listOf(
        999_999_999L,
        testData.deletedContributor.id,
        testData.disabledContributor.id,
        testData.member.id,
        testData.orgMember.id,
        testData.contributor2.id,
      ).map { userId ->
        performAuthPut(
          "/v2/projects/${testData.project.id}/invite-contributor",
          mapOf("userId" to userId, "type" to "TRANSLATE"),
        ).andIsNotFound
          .andReturn()
          .response.contentAsString
      }
    assertThat(bodies.toSet()).hasSize(1)
    assertThat(invitationService.getForProject(testData.project)).isEmpty()
  }

  @Test
  fun `invites a contributor with no name, returning a blank name and no email`() {
    recordProjectActivity(testData.unnamedContributor.id, testData.project.id)
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.unnamedContributor.id, "type" to "TRANSLATE"),
    ).andIsOk
      .andAssertThatJson {
        node("invitedUserName").isEqualTo("")
        node("invitedUserEmail").isNull()
      }
  }

  @Test
  fun `rejects inviting with the NONE role`() {
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "NONE"),
    ).andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("cannot_invite_contributor_with_no_permission")
      }
    assertThat(invitationService.getForProject(testData.project)).isEmpty()
  }

  @Test
  fun `rejects re-inviting the same contributor`() {
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "TRANSLATE"),
    ).andIsOk

    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "TRANSLATE"),
    ).andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("email_already_invited_or_member")
      }

    assertThat(invitationService.getForProject(testData.project)).hasSize(1)
  }

  @Test
  fun `binds acceptance to the invited contributor`() {
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf("userId" to testData.contributor.id, "type" to "TRANSLATE"),
    ).andIsOk
    val code = invitationService.getForProject(testData.project).single().code

    userAccount = testData.contributor2
    performAuthPut("/v2/invitations/$code/accept", null).andIsBadRequest

    userAccount = testData.contributor
    performAuthPut("/v2/invitations/$code/accept", null).andIsOk
    assertThat(invitationService.getForProject(testData.project)).isEmpty()

    val permission =
      permissionService.find(projectId = testData.project.id, userId = testData.contributor.id)
    assertThat(permission).isNotNull
    assertThat(permission!!.type).isEqualTo(ProjectPermissionType.TRANSLATE)
  }
}
