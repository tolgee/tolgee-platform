/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.AbstractSpringTest

abstract class AbstractFileStorageServiceTest : AbstractSpringTest() {

  val testFilePath = "/test/test_sub/text.txt"
  val testFileContent = "test"
}
