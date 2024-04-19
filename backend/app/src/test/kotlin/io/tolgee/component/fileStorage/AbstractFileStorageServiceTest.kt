/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
abstract class AbstractFileStorageServiceTest {
  @Autowired
  lateinit var fileStorage: FileStorage

  @Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  val testFilePath = "test/test_sub/text.txt"
  val testFileContent = "test"
}
