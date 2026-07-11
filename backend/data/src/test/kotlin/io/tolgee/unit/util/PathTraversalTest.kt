package io.tolgee.unit.util

import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.configuration.tolgee.FileStorageProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.FileStoreException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PathTraversalTest {
  @TempDir
  lateinit var tempDir: File

  private lateinit var storage: LocalFileStorage

  @BeforeEach
  fun setup() {
    val props = TolgeeProperties()
    props.fileStorage = FileStorageProperties()
    props.fileStorage.fsDataPath = tempDir.absolutePath
    storage = LocalFileStorage(props)
  }

  @Test
  fun `allows normal file paths`() {
    storage.storeFile("screenshots/test.png", "data".toByteArray())
    val result = storage.readFile("screenshots/test.png")
    assertThat(result).isEqualTo("data".toByteArray())
  }

  @Test
  fun `blocks path traversal with dot-dot-slash`() {
    assertThrows<FileStoreException> {
      storage.readFile("../../../etc/passwd")
    }
  }

  @Test
  fun `blocks path traversal in nested path`() {
    assertThrows<FileStoreException> {
      storage.readFile("screenshots/../../etc/passwd")
    }
  }

  @Test
  fun `blocks path traversal with double encoding trick`() {
    assertThrows<FileStoreException> {
      storage.readFile("....//....//etc/passwd")
    }
  }

  @Test
  fun `blocks path traversal on store`() {
    assertThrows<FileStoreException> {
      storage.storeFile("../../evil.txt", "malicious".toByteArray())
    }
  }

  @Test
  fun `blocks path traversal on delete`() {
    assertThrows<FileStoreException> {
      storage.deleteFile("../../important.dat")
    }
  }
}
