/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ImageUploadController

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.satisfies
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.secured-image-retrieval=true",
    "tolgee.authentication.secured-image-timestamp-max-age=10000"
  ]
)
class SecuredV2ImageUploadControllerTest : AbstractV2ImageUploadControllerTest() {
  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Test
  fun getScreenshotFileNoTimestamp() {
    val image = imageUploadService.store(screenshotFile, userAccount!!, null)
    performAuthGet("/uploaded-images/${image.filename}.jpg").andIsNotFound
  }

  @Test
  fun getScreenshotFileInvalidTimestamp() {
    val image = imageUploadService.store(screenshotFile, userAccount!!, null)

    val token = jwtService.emitTicket(
      userAccount!!.id,
      JwtService.TicketType.IMG_ACCESS,
      5000,
      mapOf("fileName" to image.filenameWithExtension)
    )

    moveCurrentDate(Duration.ofSeconds(10))
    performAuthGet("/uploaded-images/${image.filename}.jpg?token=$token").andIsUnauthorized
  }

  @Test
  fun getFile() {
    val image = imageUploadService.store(screenshotFile, userAccount!!, null)

    val token = jwtService.emitTicket(
      userAccount!!.id,
      JwtService.TicketType.IMG_ACCESS,
      5000,
      mapOf("fileName" to image.filenameWithExtension)
    )

    val storedImage = performAuthGet("/uploaded-images/${image.filename}.png?token=$token")
      .andIsOk.andReturn().response.contentAsByteArray

    val file = File(tolgeeProperties.fileStorage.fsDataPath + "/uploadedImages/" + image.filename + ".png")
    assertThat(storedImage).isEqualTo(file.readBytes())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun upload() {
    performStoreImage().andPrettyPrint.andIsCreated.andAssertThatJson {
      node("filename").isString.satisfies {
        val file = File(tolgeeProperties.fileStorage.fsDataPath + "/uploadedImages/" + it + ".png")
        assertThat(file).exists()
        assertThat(file.readBytes().size).isCloseTo(7365, Offset.offset(200))
      }
      node("requestFilename").isString.satisfies {
        val parts = it.split("?token=")
        val auth = jwtService.validateTicket(parts[1], JwtService.TicketType.IMG_ACCESS)
        assertThat(auth.data?.get("fileName")).isEqualTo(parts[0])
      }
    }
  }
}
