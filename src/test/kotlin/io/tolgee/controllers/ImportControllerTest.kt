package io.tolgee.controllers

import com.github.javafaker.Faker
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.ImportDto
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test
import java.util.*
import java.util.stream.Collectors
import kotlin.math.ceil
import kotlin.system.measureTimeMillis

class ImportControllerTest : SignedInControllerTest() {
    private val faker = Faker()

    companion object {
        const val IMPORT_SINGLE_TIME_LIMIT_MS: Double = 2.5
        fun getMaxTimeInMs(count: Int): Long = ceil(count.toDouble() * IMPORT_SINGLE_TIME_LIMIT_MS).toLong()
    }

    @Test
    fun basicImport() {
        val repository = dbPopulator.createBase(generateUniqueString())

        commitTransaction()

        val dto = generateData(5000)

        performPost("/api/repository/${repository.id}/import", dto)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn().asyncResult

        val language = languageService.findByAbbreviation("en", repository).orElseGet(null)!!
        val translations = translationService.getAllByLanguageId(language.id)
        assertThat(translations.stream().map { it.text }.collect(Collectors.toList())).containsAll(dto.data!!.values)
    }

    @Test
    fun streamingProgress() {
        val repository = dbPopulator.createBase(generateUniqueString())

        commitTransaction()

        val dto = generateData(50)

        val asyncResult = performPost("/api/repository/${repository.id}/import", dto)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = mvc.perform(asyncDispatch(asyncResult)).andReturn()
        assertThat(result.response.contentAsByteArray).startsWith(0, 1, 2).endsWith(dto.data!!.size - 1)
    }

    @Test
    fun basicPerformance() {
        val repository = dbPopulator.createBase(generateUniqueString())

        commitTransaction()

        val dto1 = generateData(10000)

        val timeOnEmpty = measureTimeMillis {
            performPost("/api/repository/${repository.id}/import", dto1)
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn().asyncResult
        }

        assertThat(timeOnEmpty).isLessThan(getMaxTimeInMs(dto1.data!!.size))

        val dto2 = generateData(5000)
        val timeOnFull = measureTimeMillis {
            performPost("/api/repository/${repository.id}/import", dto2)
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn().asyncResult
        }

        //if it does not import in 5 seconds, you are doing something terribly wrong!
        assertThat(timeOnFull).isLessThan(getMaxTimeInMs(dto2.data!!.size))
    }

    @Test
    fun sameData() {
        val repository = dbPopulator.createBase(generateUniqueString())

        commitTransaction()

        val dto = generateData(100)

        performPost("/api/repository/${repository.id}/import", dto)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn().asyncResult

        val language = languageService.findByAbbreviation("en", repository).orElseGet(null)!!
        var translations = translationService.getAllByLanguageId(language.id)
        assertThat(translations.stream().map { it.text }.collect(Collectors.toList())).containsAll(dto.data!!.values)

        performPost("/api/repository/${repository.id}/import", dto)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn().asyncResult

        translations = translationService.getAllByLanguageId(language.id)
        assertThat(translations.stream().map { it.text }.collect(Collectors.toList())).containsAll(dto.data!!.values)
    }

    private fun generateData(count: Int): ImportDto {
        val data = HashMap<String, String>()
        for (i in 1..count) {
            val sentence = faker.lorem().sentence()
            val key = sentence.replace(" ", "_")
                    .replace("[^A-Za-z0-9]".toRegex(), "")
                    .toLowerCase()
            data[key + i] = sentence
        }

        return ImportDto("en", data)
    }
}