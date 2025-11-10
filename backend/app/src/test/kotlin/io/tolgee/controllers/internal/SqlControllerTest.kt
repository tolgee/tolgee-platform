package io.tolgee.controllers.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(properties = ["tolgee.internal.controller-enabled=true"])
class SqlControllerTest :
  AbstractControllerTest(),
  Logging {
  @Suppress("RedundantModalityModifier")
  final inline fun <reified T> MvcResult.parseResponseTo(): T {
    return jacksonObjectMapper().readValue(this.response.contentAsString)
  }

  @Test
  fun listEndpoints() {
    val requestMappingHandlerMapping: RequestMappingHandlerMapping =
      applicationContext
        .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
    val map: Map<RequestMappingInfo, HandlerMethod> =
      requestMappingHandlerMapping
        .handlerMethods
    map.forEach { (key: RequestMappingInfo?, value: HandlerMethod?) ->
      logger.info(
        "{} {}",
        key,
        value,
      )
    }
  }

  @Test
  fun getList() {
    logger.info("Internal controller enabled: ${tolgeeProperties.internal.controllerEnabled}")
    dbPopulator.createBase()
    val parseResponseTo: List<Any> =
      mvc
        .perform(
          post("/internal/sql/list")
            .content("select * from user_account"),
        ).andExpect(status().isOk)
        .andReturn()
        .parseResponseTo()

    assertThat(parseResponseTo).isNotEmpty
  }

  @Test
  fun delete() {
    logger.info("Internal controller enabled: ${tolgeeProperties.internal.controllerEnabled}")
    val project = dbPopulator.createBase().project
    mvc
      .perform(
        post("/internal/sql/execute")
          .content("delete from permission"),
      ).andExpect(status().isOk)
      .andReturn()

    assertThat(permissionService.getAllOfProject(project)).isEmpty()
  }
}
