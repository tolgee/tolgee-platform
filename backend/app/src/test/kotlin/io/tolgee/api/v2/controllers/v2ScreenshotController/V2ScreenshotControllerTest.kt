/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import java.util.stream.Collectors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2ScreenshotControllerTest : AbstractV2ScreenshotControllerTest() {
  lateinit var initialScreenshotUrl: String

  @BeforeAll
  fun before() {
    initialScreenshotUrl = tolgeeProperties.fileStorageUrl
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialScreenshotUrl
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `uploads single screenshot`() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))

    performStoreScreenshot(project, key).andPrettyPrint.andIsCreated.andAssertThatJson {
      val screenshots = screenshotService.findAll(key = key)
      assertThat(screenshots).hasSize(1)
      node("filename").isEqualTo(screenshots[0].filename)
      val file = File(tolgeeProperties.fileStorage.fsDataPath + "/screenshots/" + screenshots[0].filename)
      assertThat(file).exists()
      assertThat(file.readBytes().size).isLessThan(1024 * 100)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not upload more then 20`() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    repeat((1..20).count()) {
      performStoreScreenshot(project, key).andIsCreated
    }
    performStoreScreenshot(project, key).andIsBadRequest
    assertThat(screenshotService.findAll(key = key)).hasSize(20)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun findAll() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val key2 = keyService.create(project, DeprecatedKeyDto("test_2"))

    screenshotService.store(screenshotFile, key)
    screenshotService.store(screenshotFile, key)
    screenshotService.store(screenshotFile, key2)

    performProjectAuthGet("keys/${key.id}/screenshots").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.screenshots").isArray.hasSize(2)
      node("_embedded.screenshots[0].filename").isString.satisfies {
        val file = File(tolgeeProperties.fileStorage.fsDataPath + "/screenshots/" + it)
        assertThat(file.exists()).isTrue()
      }
    }

    performProjectAuthGet("keys/${key2.id}/screenshots").andIsOk.andAssertThatJson {
      node("_embedded.screenshots").isArray.hasSize(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns correct fileUrl when absolute url is set`() {
    tolgeeProperties.fileStorageUrl = "http://hello.com"
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    screenshotService.store(screenshotFile, key)

    performProjectAuthGet("keys/${key.id}/screenshots").andIsOk.andPrettyPrint.andAssertThatJson {

      node("_embedded.screenshots[0].fileUrl").isString.startsWith("http://hello.com/screenshots")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun getScreenshotFile() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val screenshot = screenshotService.store(screenshotFile, key)

    val file = File(tolgeeProperties.fileStorage.fsDataPath + "/screenshots/" + screenshot.filename)
    val result = performAuthGet("/screenshots/${screenshot.filename}").andIsOk
      .andExpect(
        header().string("Cache-Control", "max-age=365, must-revalidate, no-transform")
      )
      .andReturn()
    assertThat(result.response.contentAsByteArray).isEqualTo(file.readBytes())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun delete() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))

    val list = (1..20).map {
      screenshotService.store(screenshotFile, key)
    }.toCollection(mutableListOf())

    val idsToDelete = list.stream().limit(10).map { it.id.toString() }.collect(Collectors.joining(","))

    performProjectAuthDelete("/keys/${key.id}/screenshots/$idsToDelete", null).andExpect(status().isOk)

    val rest = screenshotService.findAll(key)
    assertThat(rest).isEqualTo(list.stream().skip(10).collect(Collectors.toList()))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun uploadValidationNoImage() {
    val key = keyService.create(project, DeprecatedKeyDto("test"))
    val response = performProjectAuthMultipart(
      "keys/${key.id}/screenshots",
      listOf(
        MockMultipartFile(
          "screenshot", "originalShot.png", "not_valid",
          "test".toByteArray()
        )
      ),
    ).andIsBadRequest.andReturn()
    assertThat(response).error().isCustomValidation.hasMessage("file_not_image")
  }
}
