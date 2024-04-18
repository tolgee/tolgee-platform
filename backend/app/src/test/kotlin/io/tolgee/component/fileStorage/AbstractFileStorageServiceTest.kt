/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractFileStorageServiceTest {
  @set:Autowired
  lateinit var fileStorage: FileStorage

  @set:Autowired
  open lateinit var tolgeeProperties: TolgeeProperties

  val testFilePath = "test/test_sub/text.txt"
  val testFileContent = "test"
}
