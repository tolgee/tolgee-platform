/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ImageUploadController

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.fixtures.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.testng.annotations.Test
import java.io.File
import java.util.stream.Collectors

class V2ImageUploadControllerTest : AbstractV2ImageUploadControllerTest() {

  @Test
  fun `uploads single screenshot`() {
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
