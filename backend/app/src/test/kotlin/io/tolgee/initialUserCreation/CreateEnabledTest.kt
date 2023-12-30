/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.initialUserCreation

import io.tolgee.Application
import io.tolgee.CleanDbBeforeClass
import io.tolgee.commandLineRunners.InitialUserCreatorCommandLineRunner
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.repository.UserAccountRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.AbstractTransactionalTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.io.File

@ContextRecreatingTest
@SpringBootTest(
  classes = [Application::class],
  properties = [
    "tolgee.file-storage.fs-data-path=./build/create-enabled-test-data/",
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

  private val passwordFile = File("./build/create-enabled-test-data/initial.pwd")

  @Autowired
  lateinit var initialUserCreatorCommandLineRunner: InitialUserCreatorCommandLineRunner

  @BeforeAll
  fun callTheRunner() {
    initialUserCreatorCommandLineRunner.run()
  }

  @Test
  fun storesPassword() {
    assertThat(passwordFile).exists()
    assertThat(passwordFile.readText()).isNotBlank
  }

  @Test
  fun passwordStoredInDb() {
    val johny = userAccountService.findActive("johny")
    assertThat(passwordEncoder.matches(passwordFile.readText(), johny!!.password)).isTrue
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
    passwordFile.delete()
    resetInitialPassword()

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
}
