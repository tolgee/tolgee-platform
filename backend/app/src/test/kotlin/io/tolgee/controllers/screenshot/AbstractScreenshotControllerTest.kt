/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers.screenshot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.response.ScreenshotDTO
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

abstract class AbstractScreenshotControllerTest : AuthorizedControllerTest() {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  @AfterAll
  fun cleanUp() {
    File("${tolgeeProperties.fileStorage.fsDataPath}/screenshots").deleteRecursively()
  }

  protected fun performStoreScreenshot(project: Project, key: Key): ScreenshotDTO {
    loginAsUser("admin")
    return performAuthMultipart(
      url = "/api/project/${project.id}/screenshots",
      files = listOf(
        MockMultipartFile(
          "screenshot", "originalShot.png", "image/png",
          screenshotFile.file.readBytes()
        )
      ),
      params = mapOf("key" to arrayOf(key.name))
    )
      .andExpect(status().isOk).andReturn().parseResponseTo()
  }

  @Suppress("RedundantModalityModifier")
  protected final inline fun <reified T> MvcResult.parseResponseTo(): T {
    return jacksonObjectMapper().readValue(this.response.contentAsString)
  }
}
