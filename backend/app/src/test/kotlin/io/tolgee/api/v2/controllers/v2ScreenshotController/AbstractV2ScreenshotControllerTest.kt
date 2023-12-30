/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.util.generateImage
import org.junit.jupiter.api.AfterAll
import org.springframework.core.io.InputStreamSource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import java.io.File

abstract class AbstractV2ScreenshotControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  val screenshotFile: InputStreamSource by lazy {
    generateImage(100, 100)
  }

  @AfterAll
  fun cleanUp() {
    File("${tolgeeProperties.fileStorage.fsDataPath}/screenshots").deleteRecursively()
  }

  protected fun performStoreScreenshot(
    project: Project,
    key: Key,
    info: Any? = null,
  ): ResultActions {
    return performProjectAuthMultipart(
      url = "keys/${key.id}/screenshots",
      files =
        listOf(
          MockMultipartFile(
            "screenshot",
            "originalShot.png",
            "image/png",
            screenshotFile.inputStream.readAllBytes(),
          ),
          MockMultipartFile(
            "info",
            "info",
            MediaType.APPLICATION_JSON_VALUE,
            jacksonObjectMapper().writeValueAsBytes(info),
          ),
        ),
    )
  }

  @Suppress("RedundantModalityModifier")
  protected final inline fun <reified T> MvcResult.parseResponseTo(): T {
    return jacksonObjectMapper().readValue(this.response.contentAsString)
  }
}
