package com.polygloat.service;

import com.polygloat.development.DbPopulatorReal;
import com.polygloat.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TranslationServiceTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    TranslationService translationService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    DbPopulatorReal dbPopulator;

    @Autowired
    RepositoryService repositoryService;

    @Test
    @Transactional
    void getTranslations() {

        Repository app = dbPopulator.populate("App");


        Map<String, Object> viewData = translationService.getTranslations(new HashSet<>(Arrays.asList("en", "de")), app.getId());
        assertThat(viewData.get("en")).isInstanceOf(Map.class);
    }

    @Test
    @Transactional
    void getSourceTranslations() {
        Repository app = dbPopulator.populate("App");

     /*   Map<String, String> map = translationService.getSourceTranslations(app.getId(),
                PathDTO.fromFullPath("home.news.This_is_another_translation_in_news_folder"), parseLanguages(langs).orElse(null));
        assertThat(map.get("en")).isInstanceOf(String.class);
        map = translationService.getSourceTranslations(app.getId(),
                PathDTO.fromFullPath("Hello_world"), parseLanguages(langs).orElse(null));
        assertThat(map.get("en")).isInstanceOf(String.class);*/
    }

}
