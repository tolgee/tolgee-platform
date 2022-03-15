/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.initial_user_creation

import io.tolgee.Application
import io.tolgee.CleanDbBeforeClass
import io.tolgee.InitialUserCreatorRunner
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.UserAccountService
import io.tolgee.testing.AbstractTransactionalTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.io.File

@ContextRecreatingTest
@SpringBootTest(
  classes = [Application::class],
  properties = [
    "tolgee.file-storage.fs-data-path=./build/create-enabled-test-data/",
    "tolgee.authentication.create-initial-user=true",
    "tolgee.authentication.initialUsername=johny"
  ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@CleanDbBeforeClass
class CreateEnabledTest : AbstractTransactionalTest() {
  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  private val passwordFile = File("./build/create-enabled-test-data/initial.pwd")

  @Autowired
  lateinit var initialUserCreatorRunner: InitialUserCreatorRunner

  @BeforeAll
  fun callTheRunner() {
    initialUserCreatorRunner.run()
  }

  @Test
  fun storesPassword() {
    assertThat(passwordFile).exists()
    assertThat(passwordFile.readText()).isNotBlank
  }

  @Test
  fun passwordStoredInDb() {
    val johny = userAccountService.findOptional("johny").orElseGet(null)
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    assertThat(bCryptPasswordEncoder.matches(passwordFile.readText(), johny.password)).isTrue
  }

  @AfterAll
  fun cleanUp() {
    passwordFile.delete()
  }
}
