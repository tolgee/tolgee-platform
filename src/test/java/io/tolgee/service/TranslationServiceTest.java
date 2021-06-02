package io.tolgee.service;

import io.tolgee.development.DbPopulatorReal;
import io.tolgee.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public class TranslationServiceTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    TranslationService translationService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    DbPopulatorReal dbPopulator;

    @Autowired
    ProjectService projectService;

    @Test
    @Transactional
    void getTranslations() {

        Project app = dbPopulator.populate("App");


        Map<String, Object> viewData = translationService.getTranslations(new HashSet<>(Arrays.asList("en", "de")), app.getId());
        assertThat(viewData.get("en")).isInstanceOf(Map.class);
    }
}
