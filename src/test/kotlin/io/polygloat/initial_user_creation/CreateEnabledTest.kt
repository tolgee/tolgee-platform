/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.initial_user_creation

import io.polygloat.assertions.Assertions.assertThat
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.service.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.io.File

@AutoConfigureMockMvc
@SpringBootTest(properties = [
    "polygloat.file-storage.fs-data-path=./build/create-enabled-test-data/",
    "polygloat.authentication.create-initial-user=true",
    "polygloat.authentication.initialUsername=johny"
])
class CreateEnabledTest : AbstractTestNGSpringContextTests() {
    @set:Autowired
    lateinit var polygloatProperties: PolygloatProperties

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
        val johny = userAccountService.getByUserName("johny").orElseGet(null)
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        assertThat(bCryptPasswordEncoder.matches(passwordFile.readText(), johny.password)).isTrue
    }

    @AfterClass
    fun cleanUp() {
        passwordFile.delete()
    }
}