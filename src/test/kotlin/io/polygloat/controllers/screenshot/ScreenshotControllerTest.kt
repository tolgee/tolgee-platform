/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.controllers.screenshot

import io.polygloat.assertions.Assertions.assertThat
import io.polygloat.controllers.LoggedRequestFactory.addToken
import io.polygloat.dtos.request.GetScreenshotsByKeyDTO
import io.polygloat.dtos.response.KeyDTO
import io.polygloat.dtos.response.ScreenshotDTO
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.Test
import java.io.File
import java.util.stream.Collectors

class ScreenshotControllerTest : AbstractScreenshotControllerTest() {

    @Test
    fun uploadScreenshot() {
        val repository = dbPopulator.createBase(generateUniqueString())

        val key = keyService.create(repository, KeyDTO("test"))

        val responseBody: ScreenshotDTO = performStoreScreenshot(repository, key)

        val screenshots = screenshotService.findAll(key = key)
        assertThat(screenshots).hasSize(1)
        val file = File(polygloatProperties.fileStorage.fsDataPath + "/screenshots/" + screenshots[0].filename)
        assertThat(file).exists()
        assertThat(file.readBytes().size).isLessThan(1024 * 100)
        assertThat(responseBody.filename).isEqualTo(screenshots[0].filename)
    }

    @Test
    fun uploadMultipleScreenshots() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        repeat((1..5).count()) {
            performStoreScreenshot(repository, key)
        }
        assertThat(screenshotService.findAll(key = key)).hasSize(5)
    }

    @Test
    fun findAll() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val key2 = keyService.create(repository, KeyDTO("test_2"))

        screenshotService.store(screenshotFile, key)
        screenshotService.store(screenshotFile, key)
        screenshotService.store(screenshotFile, key2)

        var result: List<ScreenshotDTO> = performPost("/api/repository/${repository.id}/screenshots/get",
                GetScreenshotsByKeyDTO(key.name!!)).andExpect(status().isOk)
                .andReturn().parseResponseTo()

        assertThat(result).hasSize(2)

        val file = File(polygloatProperties.fileStorage.fsDataPath + "/screenshots/" + result[0].filename)
        assertThat(file).exists()

        result = performPost("/api/repository/${repository.id}/screenshots/get",
                GetScreenshotsByKeyDTO(key2.name!!)).andExpect(status().isOk)
                .andReturn().parseResponseTo()

        assertThat(result).hasSize(1)
    }

    @Test
    fun getScreenshotFile() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val screenshot = screenshotService.store(screenshotFile, key)

        val file = File(polygloatProperties.fileStorage.fsDataPath + "/screenshots/" + screenshot.filename)
        val result = performGet("/screenshots/${screenshot.filename}")
                .andExpect(status().isOk)
                .andExpect(
                        header().string("Cache-Control", "max-age=365, must-revalidate, no-transform"))
                .andReturn()

        assertThat(result.response.contentAsByteArray).isEqualTo(file.readBytes())
    }

    @Test
    fun delete() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))


        val list = (1..20).map {
            screenshotService.store(screenshotFile, key)
        }.toCollection(mutableListOf())

        val idsToDelete = list.stream().limit(10).map { it.id.toString() }.collect(Collectors.joining(","))

        performDelete("/api/repository/screenshots/$idsToDelete", null).andExpect(status().isOk)

        val rest = screenshotService.findAll(key)
        assertThat(rest).isEqualTo(list.stream().skip(10).collect(Collectors.toList()))

    }

    @Test
    fun uploadValidationNoImage() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val response = mvc.perform(addToken(multipart("/api/repository/${repository.id}/screenshots")
                .file(MockMultipartFile("screenshot", "originalShot.png", "not_valid",
                        "test".toByteArray()))
                .param("key", key.name)))
                .andExpect(status().isBadRequest).andReturn()

        assertThat(response).error().isCustomValidation.hasMessage("file_not_image")
    }

    @Test
    fun uploadValidationBlankKey() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val response = mvc.perform(addToken(multipart("/api/repository/${repository.id}/screenshots")
                .file(MockMultipartFile("screenshot", "originalShot.png", "image/png",
                        screenshotFile.file.readBytes()))))
                .andExpect(status().isBadRequest).andReturn()

        assertThat(response).error().isStandardValidation.onField("key")
    }
}