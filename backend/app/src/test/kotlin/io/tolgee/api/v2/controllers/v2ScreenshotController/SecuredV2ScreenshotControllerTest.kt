/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import io.tolgee.component.TimestampValidation
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
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
class SecuredV2ScreenshotControllerTest : AbstractV2ScreenshotControllerTest() {
  @set:Autowired
  lateinit var timestampValidation: TimestampValidation

  @Test
  fun getScreenshotFileNoTimestamp() {
    val project = dbPopulator.createBase(generateUniqueString())
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val result = performAuthGet("/screenshots/${screenshot.filename}")
      .andExpect(status().isBadRequest)
      .andReturn()

    assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
  }

  @Test
  fun getScreenshotFileInvalidTimestamp() {
    val project = dbPopulator.createBase(generateUniqueString())
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val rawTimestamp = Date().time - tolgeeProperties.authentication.securedImageTimestampMaxAge - 500
    val timestamp = timestampValidation.encryptTimeStamp(rawTimestamp)

    val result = performAuthGet("/screenshots/${screenshot.filename}?timestamp=$timestamp")
      .andExpect(status().isBadRequest)
      .andReturn()

    assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
  }

  @Test
  fun getScreenshotFile() {
    val project = dbPopulator.createBase(generateUniqueString())
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val rawTimestamp = Date().time - tolgeeProperties.authentication.securedImageTimestampMaxAge + 500
    val timestamp = timestampValidation.encryptTimeStamp(rawTimestamp)

    performAuthGet("/screenshots/${screenshot.filename}?timestamp=$timestamp")
      .andExpect(status().isOk)
      .andReturn()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun uploadScreenshot() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))

    performStoreScreenshot(project, key).andIsCreated.andAssertThatJson {
      val screenshots = screenshotService.findAll(key = key)
      assertThat(screenshots).hasSize(1)
      val file = File(tolgeeProperties.fileStorage.fsDataPath + "/screenshots/" + screenshots[0].filename)
      assertThat(file).exists()
      assertThat(file.readBytes().size).isLessThan(1024 * 100)
      node("filename").isString.startsWith(screenshots[0].filename).satisfies {
        timestampValidation.checkTimeStamp(it.split("timestamp=")[1])
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun findAll() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    screenshotService.store(screenshotFile, key)
    performProjectAuthGet("/keys/${key.id}/screenshots").andIsOk.andAssertThatJson {
      node("_embedded.screenshots[0].filename").isString.satisfies {
        timestampValidation.checkTimeStamp(it.split("timestamp=")[1])
      }
    }
  }
}
