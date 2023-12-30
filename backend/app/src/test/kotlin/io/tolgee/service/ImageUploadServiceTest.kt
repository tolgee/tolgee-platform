/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.FileStoreException
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.generateImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.InputStreamSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ImageUploadServiceTest : AbstractSpringTest() {
  val screenshotFile: InputStreamSource by lazy {
    generateImage(100, 100)
  }

  @SpyBean
  @Autowired
  lateinit var dateProvider: CurrentDateProvider

  @Test
  fun testCleanOldImages() {
    val user = dbPopulator.createUserIfNotExists("user")
    val storedOlder = imageUploadService.store(screenshotFile, user, null)
    Thread.sleep(1000)
    val storedNewer = imageUploadService.store(screenshotFile, user, null)

    whenever(dateProvider.date)
      .thenReturn(
        Date.from(
          Instant.now()
            .plus(2, ChronoUnit.HOURS)
            .minus(500, ChronoUnit.MILLIS),
        ),
      )
    imageUploadService.cleanOldImages()
    val after = imageUploadService.find(listOf(storedNewer.id, storedOlder.id))
    assertThat(after).hasSize(1)
    assertThrows<FileStoreException> {
      fileStorage.readFile(
        UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + storedOlder.filenameWithExtension,
      )
    }
    assertThat(after[0].id).isEqualTo(storedNewer.id)
  }
}
