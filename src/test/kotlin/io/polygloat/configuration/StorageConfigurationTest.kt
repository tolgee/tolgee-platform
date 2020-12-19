/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.configuration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest(properties = [
    "polygloat.file-storage.s3-enabled=true",
    "polygloat.file-storage.s3-enabled=true",
])
class StorageConfigurationTest : AbstractTestNGSpringContextTests() {

    @Test
    fun s3Configured() {

    }

}