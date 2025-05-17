package io.tolgee.security

import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
@ContextRecreatingTest
class DenyInternalTest : AbstractControllerTest() {
  @Test
  fun getListFail() {
    dbPopulator.createBase()
    val response =
      mvc.perform(
        MockMvcRequestBuilders.post("/internal/sql/list")
          .content("select * from user_account"),
      )
        .andExpect(MockMvcResultMatchers.status().isForbidden)
  }

  @Test
  fun setPropertyFail() {
    dbPopulator.createBase()
    val response =
      mvc.perform(
        MockMvcRequestBuilders.post("/internal/properties")
          .content("select * from user_account"),
      )
        .andExpect(MockMvcResultMatchers.status().isForbidden)
  }
}
