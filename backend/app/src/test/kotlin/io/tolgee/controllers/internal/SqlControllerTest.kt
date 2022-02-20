package io.tolgee.controllers.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(properties = ["tolgee.internal.controllerEnabled=true"])
class SqlControllerTest : AbstractControllerTest() {

  @Suppress("RedundantModalityModifier")
  final inline fun <reified T> MvcResult.parseResponseTo(): T {
    return jacksonObjectMapper().readValue(this.response.contentAsString)
  }

  @Test
  fun getList() {
    dbPopulator.createBase("Test")
    val parseResponseTo: List<Any> = mvc.perform(
      post("/internal/sql/list")
        .content("select * from user_account")
    )
      .andExpect(status().isOk).andReturn().parseResponseTo()

    assertThat(parseResponseTo).isNotEmpty
  }

  @Test
  fun delete() {
    val repo = dbPopulator.createBase("Test")
    mvc.perform(
      post("/internal/sql/execute")
        .content("delete from permission")
    )
      .andExpect(status().isOk).andReturn()

    assertThat(permissionService.getAllOfProject(repo)).isEmpty()
  }
}
