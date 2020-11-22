package io.polygloat.security

import io.polygloat.Assertions.Assertions
import io.polygloat.Assertions.UserApiAppAction
import io.polygloat.constants.ApiScope
import io.polygloat.controllers.AbstractUserAppApiTest
import io.polygloat.dtos.request.SetTranslationsDTO
import io.swagger.v3.oas.annotations.Hidden
import org.hibernate.Session
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.testng.annotations.Test

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = ["app.allowInternal=false"])
class DenyInternalTest : AbstractUserAppApiTest() {
    @Test
    fun getListFail() {
        dbPopulator.createBase("Test");
        val response = mvc.perform(MockMvcRequestBuilders.post("/internal/sql/list")
                .content("select * from user_account"))
                .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn().response.contentAsString

        Assertions.assertThat(response).isEqualTo("Internal access is not allowed");
    }

    @Test
    fun getDummyNoInternalFail() {
        dbPopulator.createBase("Test");
        val response = mvc.perform(MockMvcRequestBuilders.get("/dummy"))
                .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn().response.contentAsString

        Assertions.assertThat(response).isEqualTo("Internal access is not allowed");
    }
}

@RestController
@RequestMapping(value = ["dummy"])
@InternalController
class DummyController {
    @GetMapping(value = [""])
    fun getList() {
    }
}