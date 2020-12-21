/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.service

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.polygloat.assertions.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.testng.annotations.Test

const val BUCKET_NAME = "dummy_bucket_name"

@SpringBootTest(properties = [
    "polygloat.file-storage.s3.enabled=true",
    "polygloat.file-storage.s3.access-key=dummy_access_key",
    "polygloat.file-storage.s3.secret-key=dummy_secret_key",
    "polygloat.file-storage.s3.endpoint=dummy.endpoint.com",
    "polygloat.file-storage.s3.signing-region=dummy_signing_region",
    "polygloat.file-storage.s3.bucket-name=${BUCKET_NAME}",
    "polygloat.authentication.jwt-secret=dummy_jwt_secret"
])
class FileStorageServiceS3Test : AbstractFileStorageServiceTest() {
    @set:Autowired
    lateinit var s3: AmazonS3

    var s3object: S3Object = mockk()

    var s3ObjectInputStream: S3ObjectInputStream = mockk()

    @Test
    fun testGetFile() {
        every { s3.getObject(BUCKET_NAME, testFilePath) } returns s3object
        every { s3object.objectContent } returns s3ObjectInputStream
        val fileByteContent = testFileContent.toByteArray(charset("UTF-8"))
        every { s3ObjectInputStream.readAllBytes() } returns fileByteContent
        assertThat(fileStorageService.readFile(testFilePath)).isEqualTo(fileByteContent)
    }

    @Test
    fun testDeleteFile() {
        every { s3.deleteObject(BUCKET_NAME, testFilePath) } returns Unit
        fileStorageService.deleteFile(testFilePath)
        verify { s3.deleteObject(BUCKET_NAME, testFilePath) }
    }

    @Test
    fun testStoreFile() {
        every { s3.putObject(any()) } returns mockk()
        fileStorageService.storeFile(testFilePath, testFileContent.toByteArray())
        verify {
            s3.putObject(any())
        }
    }

    @Test
    fun testFileExists() {
        every { s3.putObject(any()) } returns mockk()
        fileStorageService.storeFile(testFilePath, testFileContent.toByteArray())
        verify {
            s3.putObject(any())
        }
    }
}

@Configuration
open class MockBeanConfiguration {
    @Bean
    @Primary
    open fun mockS3(): AmazonS3 {
        return mockk()
    }
}
