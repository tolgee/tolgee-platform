/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests

abstract class AbstractFileStorageServiceTest : AbstractTestNGSpringContextTests() {

  @set:Autowired
  lateinit var fileStorageService: FileStorageService

  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  val testFilePath = "/test/test_sub/text.txt"
  val testFileContent = "test"
}
