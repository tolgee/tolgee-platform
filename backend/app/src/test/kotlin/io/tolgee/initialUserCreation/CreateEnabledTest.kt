/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.initialUserCreation

import io.tolgee.Application
import io.tolgee.CleanDbBeforeClass
import io.tolgee.commandLineRunners.InitialUserCreatorCommandLineRunner
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.QuickStartRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.AbstractTransactionalTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.InMemoryFileStorage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
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
class CreateEnabledTest : AbstractTransactionalTest() {
  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  @set:Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @set:Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @set:Autowired
  lateinit var initialPasswordManager: InitialPasswordManager

  @set:Autowired
  lateinit var fileStorage: FileStorage

  @Autowired
  lateinit var initialUserCreatorCommandLineRunner: InitialUserCreatorCommandLineRunner

  @Autowired
  lateinit var projectService: ProjectService

  @Autowired
  lateinit var quickStartRepository: QuickStartRepository

  @BeforeAll
  fun callTheRunner() {
    initialUserCreatorCommandLineRunner.run()
  }

  @Test
  fun `creates demo project for initial user`() {
    val johny = userAccountService.findActive("johny")
    assertThat(johny!!.organizationRoles).hasSize(1)
    assertThat(johny.organizationRoles[0].type).isEqualTo(OrganizationRoleType.OWNER)
    val organization = johny.organizationRoles[0].organization!!
    val projects = projectService.findAllInOrganization(organizationId = organization.id)
    assertThat(projects[0].name).isEqualTo("Demo project")
  }

  @Test
  fun storesPassword() {
    assertThat(getPasswordFileContents().toString(Charsets.UTF_8)).isNotBlank
  }

  @Test
  fun passwordStoredInDb() {
    val johny = userAccountService.findActive("johny")
    assertThat(passwordEncoder.matches(getPasswordFileContents().toString(Charsets.UTF_8), johny!!.password)).isTrue
  }

  @Test
  fun passwordUpdated() {
    resetInitialPassword()

    val passBefore = userAccountService.findActive("johny")!!.password
    tolgeeProperties.authentication.initialPassword = "new password!!"
    initialUserCreatorCommandLineRunner.run()

    val johnyAfter = userAccountService.findActive("johny")
    assertThat(passBefore).isNotEqualTo(johnyAfter!!.password)
  }

  @Test
  fun passwordNotUpdatedAfterChange() {
    resetInitialPassword()

    val johnyBefore = userAccountService.findActive("johny")!!
    val passBefore = johnyBefore.password
    johnyBefore.passwordChanged = true
    userAccountService.save(johnyBefore)

    tolgeeProperties.authentication.initialPassword = "another new password!!"
    initialUserCreatorCommandLineRunner.run()

    val johnyAfter = userAccountService.findActive("johny")!!
    assertThat(passBefore).isEqualTo(johnyAfter.password)

    johnyBefore.passwordChanged = false
    userAccountService.save(johnyBefore)
  }

  @AfterAll
  fun cleanUp() {
    (fileStorage as InMemoryFileStorage).clear()
    resetInitialPassword()
    quickStartRepository.deleteAll()
    val initial = userAccountService.findActive("johny")!!
    userAccountRepository.delete(initial)
  }

  private fun resetInitialPassword() {
    val field = initialPasswordManager.javaClass.getDeclaredField("cachedInitialPassword")
    with(field) {
      isAccessible = true
      set(initialPasswordManager, null)
    }
  }

  private fun getPasswordFileContents(): ByteArray {
    return fileStorage.readFile(FILE_NAME)
  }

  companion object {
    const val FILE_NAME = "initial.pwd"
  }
}
