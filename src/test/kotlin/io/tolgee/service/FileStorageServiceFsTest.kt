/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.assertions.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

@SpringBootTest
class FileStorageServiceFsTest : AbstractFileStorageServiceTest() {

    lateinit var file: File

    @BeforeMethod
    fun beforeMethod() {
        file = File(tolgeeProperties.fileStorage.fsDataPath + testFilePath)
        file.parentFile.mkdirs()
        file.writeText(testFileContent)
    }

    @Test
    fun testReadFile() {
        val content = fileStorageService.readFile(testFilePath).toString(charset("UTF-8"))
        assertThat(content).isEqualTo(testFileContent)
    }

    @Test
    fun testDeleteFile() {
        fileStorageService.deleteFile(testFilePath)
        assertThat(file).doesNotExist()
    }

    @Test
    fun testStoreFile() {
        val filePath = "aaa/aaaa/aaa.txt"
        fileStorageService.storeFile("aaa/aaaa/aaa.txt", "hello".toByteArray(charset("UTF-8")))
        val file = File("${tolgeeProperties.fileStorage.fsDataPath}/$filePath")
        assertThat(file).exists()
        assertThat(file).hasContent("hello")
    }

    @Test
    fun testFileExists() {
        assertThat(fileStorageService.fileExists(testFilePath)).isTrue
        assertThat(fileStorageService.fileExists("not_existing")).isFalse
    }
}