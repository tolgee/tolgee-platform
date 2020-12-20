/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.controllers.screenshot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.polygloat.controllers.LoggedRequestFactory.addToken
import io.polygloat.controllers.SignedInControllerTest
import io.polygloat.dtos.response.ScreenshotDTO
import io.polygloat.model.Key
import io.polygloat.model.Repository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.AfterClass
import java.io.File

abstract class AbstractScreenshotControllerTest : SignedInControllerTest() {
    @Value("classpath:screenshot.png")
    lateinit var screenshotFile: Resource

    @AfterClass
    fun cleanUp() {
        File("${polygloatProperties.fileStorage.fsDataPath}/screenshots").deleteRecursively()
    }

    protected fun performStoreScreenshot(repository: Repository, key: Key): ScreenshotDTO {
        return mvc.perform(addToken(multipart("/api/repository/${repository.id}/screenshots")
                .file(MockMultipartFile("screenshot", "originalShot.png", "image/png",
                        screenshotFile.file.readBytes()))
                .param("key", key.name)))
                .andExpect(status().isOk).andReturn().parseResponseTo()
    }

    protected inline fun <reified T> MvcResult.parseResponseTo(): T {
        return jacksonObjectMapper().readValue(this.response.contentAsString)
    }
}