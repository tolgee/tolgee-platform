/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ScreenshotController

import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.satisfies
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.InMemoryFileStorage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyScreenshotControllerTest : AbstractV2ScreenshotControllerTest() {
  lateinit var initialScreenshotUrl: String

  @BeforeAll
  fun before() {
    initialScreenshotUrl = tolgeeProperties.fileStorageUrl
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialScreenshotUrl
    (fileStorage as InMemoryFileStorage).clear()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `uploads single screenshot`() {
    val key = keyService.create(project, CreateKeyDto("test"))

    val text = "I am key"
    performStoreScreenshot(
      project,
      key,
      info =
        mapOf(
          "positions" to
            listOf(
              mapOf(
                "x" to 200,
                "y" to 100,
                "width" to 40,
                "height" to 40,
              ),
            ),
          "text" to text,
        ),
    ).andPrettyPrint.andIsCreated.andAssertThatJson {
      executeInNewTransaction {
        val screenshots = screenshotService.findAll(key = key)
        assertThat(screenshots).hasSize(1)
        node("filename").isEqualTo(screenshots[0].filename)
        fileStorage.fileExists("screenshots/" + screenshots[0].filename).assert.isTrue()
        val reference = screenshots[0].keyScreenshotReferences[0]
        reference.originalText.assert.isEqualTo(text)
        reference.positions!![0]
          .x.assert
          .isEqualTo(200)
        reference.positions!![0]
          .y.assert
          .isEqualTo(100)
        reference.positions!![0]
          .width.assert
          .isEqualTo(40)
        reference.positions!![0]
          .height.assert
          .isEqualTo(40)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `uploads without metadata`() {
    val key = keyService.create(project, CreateKeyDto("test"))

    performStoreScreenshot(
      project,
      key,
      info = null,
    ).andIsCreated
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not upload more then 20`() {
    val key = keyService.create(project, CreateKeyDto("test"))
    repeat((1..20).count()) {
      performStoreScreenshot(project, key).andIsCreated
    }
    performStoreScreenshot(project, key).andIsBadRequest
    assertThat(screenshotService.findAll(key = key)).hasSize(20)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun findAll() {
    val (key, key2) =
      executeInNewTransaction {
        val key = keyService.create(project, CreateKeyDto("test"))
        val key2 = keyService.create(project, CreateKeyDto("test_2"))

        screenshotService.store(screenshotFile, key, null)
        screenshotService.store(screenshotFile, key, null)
        screenshotService.store(screenshotFile, key2, null)

        key to key2
      }

    performProjectAuthGet("keys/${key.id}/screenshots").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.screenshots").isArray.hasSize(2)
      node("_embedded.screenshots[0].filename").isString.satisfies {
        fileStorage.fileExists("screenshots/" + it).assert.isTrue()
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

    val key =
      executeInNewTransaction {
        val key = keyService.create(project, CreateKeyDto("test"))
        screenshotService.store(screenshotFile, key, null)
        key
      }

    performProjectAuthGet("keys/${key.id}/screenshots").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.screenshots[0].fileUrl").isString.startsWith("http://hello.com/screenshots")
    }
  }

  // Renamed from getScreenshotFile to avoid Spring bean introspection conflict
  @Test
  @ProjectJWTAuthTestMethod
  fun performGetScreenshotFile() {
    val screenshot =
      executeInNewTransaction {
        val key = keyService.create(project, CreateKeyDto("test"))
        screenshotService.store(screenshotFile, key, null)
      }
    val result =
      performGet("/screenshots/${screenshot.filename}")
        .andIsOk
        .andExpect(
          header().string("Cache-Control", "max-age=365, must-revalidate, no-transform"),
        ).andReturn()
    performGet("/screenshots/${screenshot.thumbnailFilename}").andIsOk
    assertThat(result.response.contentAsByteArray).isEqualTo(fileStorage.readFile("screenshots/" + screenshot.filename))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun delete() {
    val (key, list) =
      executeInNewTransaction {
        val key = keyService.create(project, CreateKeyDto("test"))

        val list =
          (1..20)
            .map {
              screenshotService.store(screenshotFile, key, null)
            }.toCollection(mutableListOf())
        key to list
      }
    val chunked = list.chunked(10)
    val toDelete = chunked[0]
    val notToDelete = chunked[1]

    val idsToDelete = toDelete.map { it.id.toString() }.joinToString(",")

    performProjectAuthDelete("/keys/${key.id}/screenshots/$idsToDelete", null).andExpect(status().isOk)

    val rest = screenshotService.findAll(key)
    assertThat(rest).hasSize(10).containsExactlyInAnyOrder(*notToDelete.toTypedArray())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun uploadValidationNoImage() {
    val key = keyService.create(project, CreateKeyDto("test"))
    val response =
      performProjectAuthMultipart(
        "keys/${key.id}/screenshots",
        listOf(
          MockMultipartFile(
            "screenshot",
            "originalShot.png",
            "not_valid",
            "test".toByteArray(),
          ),
        ),
      ).andIsBadRequest.andReturn()
    assertThat(response).error().isCustomValidation.hasMessage("file_not_image")
  }
}
