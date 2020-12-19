/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.controllers.screenshot

import io.polygloat.Assertions.Assertions.assertThat
import io.polygloat.component.TimestampValidation
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.dtos.request.GetScreenshotsByKeyDTO
import io.polygloat.dtos.response.KeyDTO
import io.polygloat.dtos.response.ScreenshotDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testng.annotations.Test
import java.io.File
import java.util.*

@SpringBootTest(properties = [
    "polygloat.authentication.secured-screenshot-retrieval=true",
    "polygloat.authentication.timestamp-max-age=10000"
])
class SecuredScreenshotControllerTest : AbstractScreenshotControllerTest() {
    @set:Autowired
    lateinit var timestampValidation: TimestampValidation

    @set:Autowired
    lateinit var polygloatProperties: PolygloatProperties

    @Test
    fun getScreenshotFileNoTimestamp() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val screenshot = screenshotService.store(screenshotFile, key)

        val result = performGet("/screenshots/${screenshot.filename}")
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
    }

    @Test
    fun getScreenshotFileInvalidTimestamp() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val screenshot = screenshotService.store(screenshotFile, key)

        val rawTimestamp = Date().time - polygloatProperties.authentication.timestampMaxAge - 500
        val timestamp = timestampValidation.encryptTimeStamp(rawTimestamp)

        val result = performGet("/screenshots/${screenshot.filename}?timestamp=${timestamp}")
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result).error().isCustomValidation.hasMessage("invalid_timestamp")
    }

    @Test
    fun getScreenshotFile() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        val screenshot = screenshotService.store(screenshotFile, key)

        val rawTimestamp = Date().time - polygloatProperties.authentication.timestampMaxAge + 500
        val timestamp = timestampValidation.encryptTimeStamp(rawTimestamp)

        performGet("/screenshots/${screenshot.filename}?timestamp=${timestamp}")
                .andExpect(status().isOk)
                .andReturn()
    }

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
        assertThat(responseBody.filename).startsWith(screenshots[0].filename)
        timestampValidation.checkTimeStamp(responseBody.filename.split("timestamp=")[1])
    }

    @Test
    fun findAll() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val key = keyService.create(repository, KeyDTO("test"))
        screenshotService.store(screenshotFile, key)
        val result: List<ScreenshotDTO> = performPost("/api/repository/${repository.id}/screenshots/get",
                GetScreenshotsByKeyDTO(key.name!!)).andExpect(status().isOk)
                .andReturn().parseResponseTo()
        timestampValidation.checkTimeStamp(result[0].filename.split("timestamp=")[1])
    }
}