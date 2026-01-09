/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ImageUploadController

import io.tolgee.component.MaxUploadedFilesByUserProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.satisfies
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.util.stream.Collectors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2ImageUploadControllerTest : AbstractV2ImageUploadControllerTest() {
  lateinit var initialFileStorageUrl: String

  @BeforeAll
  fun before() {
    initialFileStorageUrl = tolgeeProperties.fileStorageUrl
    whenever(maxUploadedFilesByUserProvider.invoke()).thenAnswer { 100L }
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialFileStorageUrl
  }

  @MockitoBean
  @Autowired
  lateinit var maxUploadedFilesByUserProvider: MaxUploadedFilesByUserProvider

  @Test
  fun `uploads single image`() {
    tolgeeProperties.fileStorageUrl = ""
    performStoreImage().andPrettyPrint.andIsCreated.andAssertThatJson {
      node("fileUrl").isString.startsWith("http://").endsWith(".png")
      node("requestFilename").isString.satisfies {
        fileStorage.fileExists("uploadedImages/" + it).assert.isTrue()
        fileStorage
          .readFile("uploadedImages/" + it)
          .size.assert
          .isCloseTo(5538, Offset.offset(500))
      }
    }
  }

  @Test
  fun `does not upload more then user limit`() {
    whenever(maxUploadedFilesByUserProvider.invoke()).thenAnswer { 3L }
    repeat((1..4).count()) {
      performStoreImage().andIsCreated
    }
    performStoreImage().andIsBadRequest
  }

  @Test
  fun `returns correct fileUrl when absolute url is set`() {
    tolgeeProperties.fileStorageUrl = "https://hello.com/upload"

    performStoreImage().andPrettyPrint.andIsCreated.andAssertThatJson {
      node("fileUrl").isString.startsWith("https://hello.com/upload").endsWith(".png")
    }
  }

  @Test
  fun `returns file`() {
    val image = imageUploadService.store(screenshotFile, userAccount!!, null)
    val result =
      performAuthGet("/uploaded-images/${image.filenameWithExtension}")
        .andIsOk
        .andExpect(
          header().string("Cache-Control", "max-age=365, must-revalidate, no-transform"),
        ).andReturn()
    assertThat(result.response.contentAsByteArray)
      .isEqualTo(
        fileStorage.readFile("uploadedImages/${image.filenameWithExtension}"),
      )
  }

  @Test
  fun delete() {
    whenever(maxUploadedFilesByUserProvider.invoke()).thenAnswer { 30L }
    val list =
      (1..20)
        .map {
          imageUploadService.store(screenshotFile, userAccount!!, null)
        }.toCollection(mutableListOf())

    val idsToDelete = list.take(10).map { it.id }.joinToString(",")

    list.asSequence().take(10).forEach {
      fileStorage.fileExists("uploadedImages/${it.filenameWithExtension}").assert.isTrue()
    }

    performAuthDelete("/v2/image-upload/$idsToDelete", null).andIsOk
    val rest = imageUploadService.find(list.map { it.id }.toSet())
    assertThat(rest).isEqualTo(list.stream().skip(10).collect(Collectors.toList()))

    list.asSequence().take(10).forEach {
      fileStorage.fileExists("uploadedImages/${it.filenameWithExtension}").assert.isFalse()
    }
  }

  @Test
  fun `doesn't delete when not owning`() {
    val image = imageUploadService.store(screenshotFile, dbPopulator.createUserIfNotExists("user"), null)
    performAuthDelete("/v2/image-upload/${image.id}", null).andIsForbidden
  }

  @Test
  fun uploadValidationNoImage() {
    val response =
      performAuthMultipart(
        "/v2/image-upload",
        listOf(
          MockMultipartFile(
            "image",
            "originalShot.png",
            "not_valid",
            "test".toByteArray(),
          ),
        ),
      ).andIsBadRequest.andReturn()
    assertThat(response).error().isCustomValidation.hasMessage("file_not_image")
  }
}
