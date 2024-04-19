/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest(
  properties = ["tolgee.internal.use-in-memory-file-storage=false"],
)
class FileStorageFsTest : AbstractFileStorageServiceTest() {
  lateinit var file: File

  @BeforeEach
  fun beforeMethod() {
    file = File(tolgeeProperties.fileStorage.fsDataPath + "/" + testFilePath)
    file.parentFile.mkdirs()
    file.writeText(testFileContent)
  }

  @Test
  fun `is LocalFileStorage`() {
    assertThat(fileStorage is LocalFileStorage).isTrue()
  }

  @Test
  fun testReadFile() {
    val content = fileStorage.readFile(testFilePath).toString(charset("UTF-8"))
    assertThat(content).isEqualTo(testFileContent)
  }

  @Test
  fun testDeleteFile() {
    fileStorage.deleteFile(testFilePath)
    assertThat(file).doesNotExist()
  }

  @Test
  fun testStoreFile() {
    val filePath = "aaa/aaaa/aaa.txt"
    fileStorage.storeFile("aaa/aaaa/aaa.txt", "hello".toByteArray(charset("UTF-8")))
    val file = File("${tolgeeProperties.fileStorage.fsDataPath}/$filePath")
    assertThat(file).exists()
    assertThat(file).hasContent("hello")
  }

  @Test
  fun testPruneDirectory() {
    val filePath = "aaa/aaaa/aaa.txt"
    fileStorage.storeFile(filePath, "hello".toByteArray(charset("UTF-8")))
    fileStorage.pruneDirectory("aaa")
    fileStorage.fileExists(filePath).assert.isFalse()
  }

  @Test
  fun testFileExists() {
    assertThat(fileStorage.fileExists(testFilePath)).isTrue
    assertThat(fileStorage.fileExists("not_existing")).isFalse
  }
}
