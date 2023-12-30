/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.v2ImageUploadController

import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.generateImage
import org.junit.jupiter.api.AfterAll
import org.springframework.core.io.InputStreamSource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import java.io.File

abstract class AbstractV2ImageUploadControllerTest : AuthorizedControllerTest() {
  val screenshotFile: InputStreamSource by lazy {
    generateImage(1000, 1000)
  }

  @AfterAll
  fun cleanUp() {
    File("${tolgeeProperties.fileStorage.fsDataPath}/uploadedImages").deleteRecursively()
  }

  protected fun performStoreImage(): ResultActions {
    return performAuthMultipart(
      url = "/v2/image-upload",
      files =
        listOf(
          MockMultipartFile(
            "image",
            "originalShot.png",
            "image/png",
            screenshotFile.inputStream,
          ),
        ),
    )
  }
}
