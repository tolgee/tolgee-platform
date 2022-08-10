/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers.screenshot

import io.tolgee.component.TimestampValidation
import io.tolgee.dtos.request.screenshot.GetScreenshotsByKeyDto
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.dtos.response.ScreenshotDTO
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import java.util.*

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.secured-image-retrieval=true",
    "tolgee.authentication.secured-image-timestamp-max-age=10000"
  ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecuredScreenshotControllerTest : AbstractScreenshotControllerTest() {
  @set:Autowired
  lateinit var timestampValidation: TimestampValidation

  @Test
  fun getScreenshotFileNoTimestamp() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val result = performAuthGet("/screenshots/${screenshot.filename}")
      .andExpect(status().isBadRequest)
      .andReturn()

    assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
  }

  @Test
  fun getScreenshotFileInvalidTimestamp() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val rawTimestamp = Date().time - tolgeeProperties.authentication.securedImageTimestampMaxAge - 500
    val timestamp = timestampValidation.encryptTimeStamp(screenshot.filename, rawTimestamp)

    val result = performAuthGet("/screenshots/${screenshot.filename}?timestamp=$timestamp")
      .andExpect(status().isBadRequest)
      .andReturn()

    assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
  }

  @Test
  fun getScreenshotFile() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val rawTimestamp = Date().time - tolgeeProperties.authentication.securedImageTimestampMaxAge + 500
    val timestamp = timestampValidation.encryptTimeStamp(screenshot.filename, rawTimestamp)

    performAuthGet("/screenshots/${screenshot.filename}?timestamp=$timestamp")
      .andExpect(status().isOk)
      .andReturn()
  }

  @Test
  fun uploadScreenshot() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project

    val key = keyService.create(project, DeprecatedKeyDto("test"))

    val responseBody: ScreenshotDTO = performStoreScreenshot(project, key)

    val screenshots = screenshotService.findAll(key = key)
    assertThat(screenshots).hasSize(1)
    val file = File(tolgeeProperties.fileStorage.fsDataPath + "/screenshots/" + screenshots[0].filename)
    assertThat(file).exists()
    assertThat(file.readBytes().size).isEqualTo(138412)
    assertThat(responseBody.filename).startsWith(screenshots[0].filename)
    val parts = responseBody.filename.split("?timestamp=")
    timestampValidation.checkTimeStamp(parts[0], parts[1])
  }

  @Test
  fun findAll() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    screenshotService.store(screenshotFile, key)
    val result: List<ScreenshotDTO> = performAuthPost(
      "/api/project/${project.id}/screenshots/get",
      GetScreenshotsByKeyDto(key.name)
    ).andExpect(status().isOk)
      .andReturn().parseResponseTo()
    val parts = result[0].filename.split("?timestamp=")
    timestampValidation.checkTimeStamp(parts[0], parts[1])
  }
}
