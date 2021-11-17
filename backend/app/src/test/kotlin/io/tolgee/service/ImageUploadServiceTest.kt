/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.FileStoreException
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.testng.annotations.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest
class ImageUploadServiceTest : AbstractSpringTest() {
  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

  @MockBean
  @Autowired
  lateinit var dateProvider: CurrentDateProvider

  @Test
  fun testCleanOldImages() {
    val user = dbPopulator.createUserIfNotExists("user")
    val storedOlder = imageUploadService.store(screenshotFile, user)
    Thread.sleep(5000)
    val storedNewer = imageUploadService.store(screenshotFile, user)

    whenever(dateProvider.getDate())
      .thenReturn(
        Date.from(
          Instant.now()
            .plus(2, ChronoUnit.HOURS)
            .minus(2, ChronoUnit.SECONDS)
        )
      )
    imageUploadService.cleanOldImages()
    val after = imageUploadService.find(listOf(storedNewer.id, storedOlder.id))
    assertThat(after).hasSize(1)
    assertThrows<FileStoreException> {
      fileStorageService.readFile(
        UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + storedOlder.filenameWithExtension
      )
    }
    assertThat(after[0].id).isEqualTo(storedNewer.id)
  }
}
