package io.polygloat.controllers

import com.github.javafaker.Faker
import io.polygloat.Assertions.Assertions.assertThat
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test
import java.util.*
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis

class ImportControllerTest : SignedInControllerTest() {
    private val faker = Faker()

    @Test
    fun testDoImport() {
        val repository = dbPopulator.createBase(generateUniqueString())

        commitTransaction()

        val data = HashMap<String, String>()
        val count = 50000;
        for (i in 1..count) {
            val sentence = faker.lorem().sentence()
            val source = sentence.replace(" ", "_")
                    .replace("[^A-Za-z0-9]".toRegex(), "")
                    .toLowerCase()
            data[source + i] = sentence
        }

        val dto = ImportDto("en", data)

        val timeOnEmpty = measureTimeMillis {
            performPost("/api/repository/${repository.id}/import", dto)
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn().getAsyncResult(20000)
        }

        val timeOnFull = measureTimeMillis {
            val asyncResult = performPost("/api/repository/${repository.id}/import", dto)
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn();


            val result = mvc.perform(asyncDispatch(asyncResult)).andReturn()
            assertThat(result.response.contentAsByteArray).startsWith(0, 1, 2).endsWith(count - 1)
        }

        val language = languageService.findByAbbreviation("en", repository).orElseGet(null)!!
        val translations = translationService.getAllByLanguageId(language.id)
        assertThat(translations.stream().map { it.text }.collect(Collectors.toList())).containsAll(data.values)

        //if it does not import in 20 seconds, you are doing something terribly wrong!
        assertThat(timeOnEmpty).isLessThan(20000)
        assertThat(timeOnFull).isLessThan(20000)
    }
}