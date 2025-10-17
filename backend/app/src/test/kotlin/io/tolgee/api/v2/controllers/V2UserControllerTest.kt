package io.tolgee.api.v2.controllers

import io.tolgee.config.TestEmailConfiguration
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.testDataBuilder.data.SensitiveOperationProtectionTestData
import io.tolgee.development.testDataBuilder.data.UserDeletionTestData
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNoContent
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.satisfies
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.NotificationType.PASSWORD_CHANGED
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.NotificationTestUtil
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@ContextRecreatingTest
@SpringBootTest
@Import(TestEmailConfiguration::class)
class V2UserControllerTest : AuthorizedControllerTest() {
  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  private lateinit var notificationUtil: NotificationTestUtil

  @BeforeEach
  fun init() {
    emailTestUtil.initMocks()
    notificationUtil.init()
  }

  @Test
  fun `it updates the user profile`() {
    val requestDTO =
      UserUpdateRequestDto(
        email = "ben@ben.aa",
        name = "Ben's new name",
        currentPassword = initialPassword,
      )
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findActive(requestDTO.email)
    Assertions.assertThat(fromDb!!.name).isEqualTo(requestDTO.name)
  }

  @Test
  fun `it updates the user password`() {
    val requestDTO =
      UserUpdatePasswordRequestDto(
        password = "super new password",
        currentPassword = initialPassword,
      )
    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findActive(initialUsername)
    Assertions
      .assertThat(passwordEncoder.matches(requestDTO.password, fromDb!!.password))
      .describedAs("Password is changed")
      .isTrue

    notificationUtil.newestInAppNotification().also {
      assertThat(it.type).isEqualTo(PASSWORD_CHANGED)
      assertThat(it.user.id).isEqualTo(userAccount?.id)
      assertThat(it.originatingUser?.id).isEqualTo(userAccount?.id)
    }
    assertThat(notificationUtil.newestEmailNotification()).contains("Password has been changed for your account")
  }

  @Test
  fun `it validates the user update request data`() {
    var requestDTO =
      UserUpdateRequestDto(
        email = "ben@ben.aa",
        name = "",
        currentPassword = initialPassword,
      )
    var mvcResult =
      performAuthPut("/v2/user", requestDTO)
        .andIsBadRequest
        .andReturn()
    val standardValidation = assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("name")

    requestDTO =
      UserUpdateRequestDto(
        email = "ben@ben.aa",
        name = "a",
        currentPassword = initialPassword,
      )
    dbPopulator.createUserIfNotExists(requestDTO.email)
    mvcResult =
      performAuthPut("/v2/user", requestDTO)
        .andIsBadRequest
        .andReturn()
    assertThat(mvcResult)
      .error()
      .isCustomValidation
      .hasMessage("username_already_exists")
  }

  @Test
  fun `it validates the password change request data`() {
    val requestDto =
      UserUpdatePasswordRequestDto(
        password = "",
        currentPassword = initialPassword,
      )
    val mvcResult =
      performAuthPut("/v2/user/password", requestDto)
        .andIsBadRequest
        .andReturn()
    val standardValidation = assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("password")
  }

  @Test
  fun `it sends an email when updating user email`() {
    val oldNeedsVerification = tolgeeProperties.authentication.needsEmailVerification
    tolgeeProperties.authentication.needsEmailVerification = true

    val requestDTO =
      UserUpdateRequestDto(
        email = "ben@ben.aaa",
        name = "Ben Ben",
        currentPassword = initialPassword,
      )
    performAuthPut("/v2/user", requestDTO).andIsOk

    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.verifyEmailSent()
    }
    assertThat(emailTestUtil.messageContents.single())
      .contains(tolgeeProperties.frontEndUrl.toString())

    tolgeeProperties.authentication.needsEmailVerification = oldNeedsVerification
  }

  @Test
  fun `it doesn't allow updating the email without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "a", email = "ben@ben.zz")
    performAuthPut("/v2/user", requestDTO).andIsBadRequest
  }

  @Test
  fun `it doesn't allow updating the email with an invalid password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "a", email = "ben@ben.zz", currentPassword = "meow meow meow")
    performAuthPut("/v2/user", requestDTO).andIsBadRequest
  }

  @Test
  fun `it doesn't allow updating the password without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdatePasswordRequestDto(password = "vewy secuwe paffword")
    performAuthPut("/v2/user/password", requestDTO).andIsBadRequest
  }

  @Test
  fun `it doesn't allow updating the password with an invalid password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdatePasswordRequestDto(password = "vewy secuwe paffword", currentPassword = "meow meow meow")
    performAuthPut("/v2/user/password", requestDTO).andIsForbidden
  }

  @Test
  fun `it allows updating the display name without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "zzz", email = "ben@ben.aa")
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
  }

  @Test
  fun `it invalidates tokens generated prior a password change`() {
    val requestDTO =
      UserUpdatePasswordRequestDto(
        password = "super new password",
        currentPassword = initialPassword,
      )

    loginAsAdminIfNotLogged()
    Thread.sleep(2000)

    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    refreshUser()
    performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  fun `it deletes user`() {
    val testData = UserDeletionTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.franta
    performAuthDelete("/v2/user").andIsOk
    userAccountService.findActive(testData.franta.id).assert.isNull()
    permissionService.findById(testData.frantasPermissionInOlgasProject.id).assert.isNull()
    translationCommentService.find(testData.frantasComment.id).assert.isNotNull
    patService.find(testData.frantasPat.id).assert.isNull()
    apiKeyService.find(testData.frantasApiKey.id).assert.isNull()
    organizationRoleService.find(testData.frantasRole.id).assert.isNull()
    // deletes organization with single owner
    organizationService.find(testData.frantasOrganization.id).assert.isNull()
    // doesn't delete organization with multiple owners
    organizationService.find(testData.pepaFrantaOrganization.id).assert.isNotNull
    val deleted = userAccountService.getAllByIdsIncludingDeleted(setOf(testData.franta.id)).single()
    deleted.name.assert.isEqualTo("Former user")
    deleted.username.assert.isEqualTo("former")
  }

  @Test
  fun `returns correct single owned organizations`() {
    val testData = UserDeletionTestData()
    testDataService.saveTestData(testData.root)
    assertSingleOwned(testData.franta, listOf("Franta"))
    assertSingleOwned(testData.olga, listOf("Olga"))
    executeInNewTransaction {
      userAccountService.get(testData.pepa.id).organizationRoles
    }
    executeInNewTransaction {
      userAccountService.delete(userAccountService.get(testData.franta.id))
    }
    assertSingleOwned(testData.pepa, listOf("Pepa's and Franta's org"))
  }

  @Test
  fun `it deletes member user and keeps not owning org`() {
    val testData = UserDeletionTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.olga
    performAuthDelete("/v2/user").andIsOk
    userAccountService.findActive(testData.olga.id).assert.isNull()
    organizationService.find(testData.pepaFrantaOrganization.id).assert.isNotNull
  }

  @Test
  fun `it generates super token (with password)`() {
    performAuthPost(
      "/v2/user/generate-super-token",
      mapOf(
        "password" to initialPassword,
      ),
    ).andIsOk.andAssertThatJson {
      node("accessToken").isString.satisfies { token ->
        val authentication = jwtService.validateToken(token)
        assertThat(authentication.isSuperToken).isTrue
      }
    }
  }

  @Test
  fun `it generates super token (with OTP)`() {
    val testData = SensitiveOperationProtectionTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.pepa
    performAuthPost(
      "/v2/user/generate-super-token",
      mapOf(
        "otp" to mfaService.generateStringCode(SensitiveOperationProtectionTestData.TOTP_KEY),
      ),
    ).andIsOk.andAssertThatJson {
      node("accessToken").isString.satisfies { token ->
        val authentication = jwtService.validateToken(token)
        assertThat(authentication.isSuperToken).isTrue
      }
    }
  }

  @Test
  fun `it returns no content for sso info`() {
    // EE dependant endpoint - without EE always returns no content
    performAuthGet("/v2/user/sso").andIsNoContent
  }

  @Test
  fun `it returns no content for managed by`() {
    // EE dependant endpoint - without EE always returns no content
    performAuthGet("/v2/user/managed-by").andIsNoContent
  }

  private fun assertSingleOwned(
    user: UserAccount,
    names: List<String>,
  ) {
    userAccount = user
    performAuthGet("/v2/user/single-owned-organizations").andIsOk.andAssertThatJson {
      node("_embedded.organizations") {
        isArray.hasSize(names.size)
        names.forEachIndexed { idx, name ->
          node("[$idx]") {
            node("name").isEqualTo(name)
          }
        }
      }
    }
  }
}
