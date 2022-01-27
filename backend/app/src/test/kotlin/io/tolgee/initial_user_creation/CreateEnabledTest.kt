/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.initial_user_creation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.UserAccountService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.io.File

@AutoConfigureMockMvc
@SpringBootTest(
  properties = [
    "tolgee.file-storage.fs-data-path=./build/create-enabled-test-data/",
    "tolgee.authentication.create-initial-user=true",
    "tolgee.authentication.initialUsername=johny"
  ]
)
class CreateEnabledTest : AbstractTestNGSpringContextTests() {
  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  private val passwordFile = File("./build/create-enabled-test-data/initial.pwd")

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

  @AfterClass
  fun cleanUp() {
    passwordFile.delete()
  }
}
