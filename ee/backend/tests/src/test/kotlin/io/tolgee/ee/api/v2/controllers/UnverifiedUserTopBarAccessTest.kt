package io.tolgee.ee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

// The top bar decides what to offer an unverified user from `isEmailVerified` alone, so what these
// endpoints answer is the contract it mirrors.
class UnverifiedUserTopBarAccessTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  private lateinit var testData: BaseTestData

  // Deleting the EmailVerification row directly throws a Hibernate
  // TransientPropertyValueException on commit, because UserAccount.emailVerification is the
  // inverse side of the association.
  private var pendingVerificationCode: String? = null

  @BeforeEach
  fun setup() {
    testData = BaseTestData("unverified_top_bar", "Unverified top bar project")
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    emailTestUtil.initMocks()
    tolgeeProperties.authentication.needsEmailVerification = true
    tolgeeProperties.frontEndUrl = "https://dummy-url.com"
    executeInNewTransaction {
      val emailVerification =
        emailVerificationService.createForUser(userAccountService.findActive(testData.user.id)!!)
      pendingVerificationCode = emailVerification!!.code
    }
  }

  @AfterEach
  fun cleanup() {
    pendingVerificationCode?.let { code ->
      executeInNewTransaction {
        emailVerificationService.verify(testData.user.id, code)
      }
    }
    pendingVerificationCode = null
    testDataService.cleanTestData(testData.root)
    tolgeeProperties.authentication.needsEmailVerification = false
    tolgeeProperties.frontEndUrl = null
  }

  @Test
  fun `refuses the notifications the top bar stops offering`() {
    performAuthGet("/v2/notification?size=1&filterSeen=false").andIsForbidden.andAssertThatJson {
      node("code").isEqualTo("email_not_verified")
    }
  }

  @Test
  fun `refuses the tasks the user menu stops offering`() {
    performAuthGet("/v2/user-tasks?size=1").andIsForbidden.andAssertThatJson {
      node("code").isEqualTo("email_not_verified")
    }
  }

  @Test
  fun `still serves the account settings the user menu keeps offering`() {
    performAuthGet("/v2/user").andIsOk
  }
}
