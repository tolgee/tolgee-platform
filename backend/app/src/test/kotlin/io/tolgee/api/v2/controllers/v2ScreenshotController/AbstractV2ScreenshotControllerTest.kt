/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import java.io.File

abstract class AbstractV2ScreenshotControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  @AfterAll
  fun cleanUp() {
    File("${tolgeeProperties.fileStorage.fsDataPath}/screenshots").deleteRecursively()
  }

  protected fun performStoreScreenshot(project: Project, key: Key): ResultActions {
    return performProjectAuthMultipart(
      url = "keys/${key.id}/screenshots",
      files = listOf(
        MockMultipartFile(
          "screenshot", "originalShot.png", "image/png",
          screenshotFile.file.readBytes()
        )
      )
    )
  }

  @Suppress("RedundantModalityModifier")
  protected final inline fun <reified T> MvcResult.parseResponseTo(): T {
    return jacksonObjectMapper().readValue(this.response.contentAsString)
  }
}
