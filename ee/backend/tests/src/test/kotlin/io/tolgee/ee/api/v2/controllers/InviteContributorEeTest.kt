package io.tolgee.ee.api.v2.controllers

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InviteContributorEeTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.GRANULAR_PERMISSIONS)
    emailTestUtil.initMocks()
    testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
    recordProjectActivity(testData.contributor.id, testData.project.id)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `routes a scoped contributor invite through the EE service and still hides the email`() {
    userAccount = testData.admin
    performAuthPut(
      "/v2/projects/${testData.project.id}/invite-contributor",
      mapOf(
        "userId" to testData.contributor.id,
        "scopes" to listOf("translations.edit"),
      ),
    ).andIsOk
      .andAssertThatJson {
        node("invitedUserEmail").isNull()
      }

    executeInNewTransaction {
      val invitation = invitationService.getForProject(testData.project).single()
      invitation.emailHidden.assert.isTrue()
      invitation.email.assert.isEqualTo(testData.contributor.username)
      invitation.permission!!
        .scopes.assert
        .containsExactlyInAnyOrder(Scope.TRANSLATIONS_EDIT)
    }
  }
}
