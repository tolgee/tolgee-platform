package io.tolgee.controllers

import io.tolgee.ITest
import io.tolgee.dtos.request.UserUpdateRequestDTO
import org.assertj.core.api.Assertions
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

class UserControllerTest : SignedInControllerTest(), ITest {
    @Test
    fun updateUser() {
        val requestDTO = UserUpdateRequestDTO(
                email = "ben@ben.aa",
                password = "super new password",
                name = "Ben's new name"
        )
        performAuthPost("/api/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
        val fromDb = userAccountService.getByUserName(requestDTO.email)
        Assertions.assertThat(fromDb).isNotEmpty
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        Assertions.assertThat(bCryptPasswordEncoder.matches(requestDTO.password, fromDb.get().password)).describedAs("Password is changed").isTrue
        Assertions.assertThat(fromDb.get().name).isEqualTo(requestDTO.name)
    }

    @Test
    fun updateUserValidation() {
        var requestDTO = UserUpdateRequestDTO(
                email = "ben@ben.aa",
                password = "",
                name = ""
        )
        var mvcResult = performAuthPost("/api/user", requestDTO).andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        val standardValidation = io.tolgee.assertions.Assertions.assertThat(mvcResult).error().isStandardValidation
        standardValidation.onField("password")
        standardValidation.onField("name")

        requestDTO = UserUpdateRequestDTO(
                email = "ben@ben.aa",
                password = "aksjhd  dasdsa",
                name = "a"
        )
        dbPopulator.createUserIfNotExists(requestDTO.email!!)
        mvcResult = performAuthPost("/api/user", requestDTO).andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        io.tolgee.assertions.Assertions.assertThat(mvcResult).error().isCustomValidation.hasMessage("username_already_exists")
    }
}
