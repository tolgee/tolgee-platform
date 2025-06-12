/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.initialUserCreation

import io.tolgee.AbstractSpringTest
import io.tolgee.Application
import io.tolgee.CleanDbBeforeClass
import io.tolgee.commandLineRunners.InitialUserCreatorCommandLineRunner
import io.tolgee.development.testDataBuilder.data.ImplicitUserLegacyData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.satisfies
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.security.SecurityService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@ContextRecreatingTest
@SpringBootTest(
  classes = [Application::class],
  properties = [
    "tolgee.authentication.initial-username=johny",
    "tolgee.internal.disable-initial-user-creation=false",
  ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@CleanDbBeforeClass
class LegacyMigrationTest : AbstractSpringTest() {
  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var securityService: SecurityService

  @Autowired
  lateinit var initialUserCreatorCommandLineRunner: InitialUserCreatorCommandLineRunner

  @Test
  fun `it migrates the old implicit user when creating the user account`() {
    userAccountService.findActive("johny")?.let {
      userAccountService.delete(it)
      // Also delete with the repository to not "soft delete" it.
      userAccountRepository.delete(it)
    }
    userAccountService.findActive("___implicit_user")?.let {
      userAccountService.delete(it)
      // Also delete with the repository to not "soft delete" it.
      userAccountRepository.delete(it)
    }

    val testData = ImplicitUserLegacyData()
    testDataService.saveTestData(testData.root)

    assertThat(userAccountService.findActive("johny")).isNull()
    assertThat(userAccountService.findActive("___implicit_user")).isNotNull

    initialUserCreatorCommandLineRunner.run()

    val initialUser = userAccountService.findActive("johny")!!

    assertThat(initialUser).isNotNull.satisfies { assertThat(it?.isInitialUser).isTrue() }
    assertThat(userAccountService.findActive("___implicit_user")).isNull()

    // Verify that all the things from the implicit user have been ported
    assertThat(apiKeyService.find(testData.pak.id)).isNotNull.satisfies {
      assertThat(it?.userAccount?.username).isEqualTo("johny")
    }
    assertThat(patService.find(testData.pat1.id)).isNotNull.satisfies {
      assertThat(it?.userAccount?.username).isEqualTo("johny")
    }
    assertThat(patService.find(testData.pat2.id)).isNotNull.satisfies {
      assertThat(it?.userAccount?.username).isEqualTo("johny")
    }

    assertThat(organizationService.find(testData.coolOrganization.id)).isNotNull
    assertThat(organizationRoleService.findType(initialUser.id, testData.coolOrganization.id))
      .isEqualTo(OrganizationRoleType.OWNER)

    assertDoesNotThrow {
      securityService.checkProjectPermissionNoApiKey(
        testData.randomProject.id,
        Scope.SCREENSHOTS_UPLOAD,
        UserAccountDto.fromEntity(initialUser),
      )
    }
  }
}
