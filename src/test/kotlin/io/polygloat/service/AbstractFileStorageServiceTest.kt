/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.service

import io.polygloat.configuration.polygloat.PolygloatProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests

abstract class AbstractFileStorageServiceTest : AbstractTestNGSpringContextTests() {

    @set:Autowired
    lateinit var fileStorageService: FileStorageService

    @set:Autowired
    lateinit var polygloatProperties: PolygloatProperties

    val testFilePath = "/test/test_sub/text.txt"
    val testFileContent = "test"
}