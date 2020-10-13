package com.polygloat.controllers;

import com.github.javafaker.Faker;
import com.polygloat.dtos.PathDTO;
import com.polygloat.dtos.request.ImportDto;
import com.polygloat.model.Repository;
import lombok.SneakyThrows;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.polygloat.Assertions.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ImportControllerTest extends SignedInControllerTest {

    private Faker faker = new Faker();

    @SneakyThrows
    @Test
    public void testDoImport() {
        Repository base = dbPopulator.createBase(generateUniqueString());

        HashMap<String, String> data = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            String sentence = faker.lorem().sentence();
            logger.info(String.format("Creating sentence: %s", sentence));
            String source = sentence.substring(0).replace(" ", "_").replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            data.put(source, sentence);
        }

        ImportDto dto = ImportDto.builder().languageAbbreviation("en").data(data).build();
        performPost("/api/repository/" + base.getId() + "/import", dto).andExpect(status().isOk());

        for (Map.Entry<String, String> entry : data.entrySet()) {
            Map<String, String> translations = translationService.getSourceTranslationsResult(base.getId(), PathDTO.fromFullPath(entry.getKey()), Set.of("en"));
            assertThat(translations.get("en")).isEqualTo(entry.getValue());
        }

    }
}