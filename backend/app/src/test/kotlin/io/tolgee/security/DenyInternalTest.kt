package io.tolgee.security

import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@AutoConfigureMockMvc
@SpringBootTest(properties = ["app.allowInternal=false"])
@ContextRecreatingTest
class DenyInternalTest : AbstractControllerTest() {
  @Test
  fun getListFail() {
    dbPopulator.createBase("Test")
    val response = mvc.perform(
      MockMvcRequestBuilders.post("/internal/sql/list")
        .content("select * from user_account")
    )
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn().response.contentAsString

    Assertions.assertThat(response).isEqualTo("Internal access is not allowed")
  }

  @Test
  fun getDummyNoInternalFail() {
    dbPopulator.createBase("Test")
    val response = mvc.perform(MockMvcRequestBuilders.get("/dummy"))
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn().response.contentAsString

    Assertions.assertThat(response).isEqualTo("Internal access is not allowed")
  }

  @Test
  fun setPropertyFail() {
    dbPopulator.createBase("Test")
    val response = mvc.perform(
      MockMvcRequestBuilders.post("/internal/properties")
        .content("select * from user_account")
    )
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn().response.contentAsString

    Assertions.assertThat(response).isEqualTo("Internal access is not allowed")
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
