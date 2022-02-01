/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ImageUploadController

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.io.File
import java.util.stream.Collectors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2ImageUploadControllerTest : AbstractV2ImageUploadControllerTest() {

  lateinit var initialFileStorageUrl: String

  @BeforeAll
  fun before() {
    initialFileStorageUrl = tolgeeProperties.fileStorageUrl
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialFileStorageUrl
  }

  @Test
  fun `uploads single image`() {
    tolgeeProperties.fileStorageUrl = ""
    performStoreImage().andPrettyPrint.andIsCreated.andAssertThatJson {
      node("fileUrl").isString.startsWith("http://").endsWith(".jpg")
      node("requestFilename").isString.satisfies {
        val file = File(tolgeeProperties.fileStorage.fsDataPath + "/uploadedImages/" + it)
        assertThat(file).exists()
        assertThat(file.readBytes().size).isLessThan(1024 * 100)
      }
    }
  }

  @Test
  fun `does not upload more then 100`() {
    repeat((1..101).count()) {
      performStoreImage().andIsCreated
    }
    performStoreImage().andIsBadRequest
  }

  @Test
  fun `returns correct fileUrl when absolute url is set`() {
    tolgeeProperties.fileStorageUrl = "https://hello.com/upload"

    performStoreImage().andPrettyPrint.andIsCreated.andAssertThatJson {
      node("fileUrl").isString.startsWith("https://hello.com/upload").endsWith(".jpg")
    }
  }

  @Test
  fun `returns file`() {
    val image = imageUploadService.store(screenshotFile, userAccount!!)

    val file = File("""${tolgeeProperties.fileStorage.fsDataPath}/uploadedImages/${image.filename}.jpg""")
    val result = performAuthGet("/uploaded-images/${image.filename}.jpg").andIsOk
      .andExpect(
        header().string("Cache-Control", "max-age=365, must-revalidate, no-transform")
      )
      .andReturn()
    assertThat(result.response.contentAsByteArray).isEqualTo(file.readBytes())
  }

  @Test
  fun delete() {
    val list = (1..20).map {
      imageUploadService.store(screenshotFile, userAccount!!)
    }.toCollection(mutableListOf())

    val idsToDelete = list.take(10).map { it.id }.joinToString(",")

    list.asSequence().take(10).forEach {
      assertThat(
        File("""${tolgeeProperties.fileStorage.fsDataPath}/uploadedImages/${it.filename}.jpg""")
      ).exists()
    }

    performAuthDelete("/v2/image-upload/$idsToDelete", null).andIsOk
    val rest = imageUploadService.find(list.map { it.id }.toSet())
    assertThat(rest).isEqualTo(list.stream().skip(10).collect(Collectors.toList()))

    list.asSequence().take(10).forEach {
      assertThat(
        File("""${tolgeeProperties.fileStorage.fsDataPath}/uploadedImages/${it.filename}.jpg""")
      ).doesNotExist()
    }
  }

  @Test
  fun `doesn't delete when not owning`() {
    val image = imageUploadService.store(screenshotFile, dbPopulator.createUserIfNotExists("user"))
    performAuthDelete("/v2/image-upload/${image.id}", null).andIsForbidden
  }

  @Test
  fun uploadValidationNoImage() {
    val response = performAuthMultipart(
      "/v2/image-upload",
      listOf(
        MockMultipartFile(
          "image", "originalShot.png", "not_valid",
          "test".toByteArray()
        )
      ),
    ).andIsBadRequest.andReturn()
    assertThat(response).error().isCustomValidation.hasMessage("file_not_image")
  }
}
