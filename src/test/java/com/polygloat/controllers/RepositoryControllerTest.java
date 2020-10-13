package com.polygloat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polygloat.dtos.request.AbstractRepositoryDTO;
import com.polygloat.dtos.request.CreateRepositoryDTO;
import com.polygloat.dtos.request.EditRepositoryDTO;
import com.polygloat.dtos.request.LanguageDTO;
import com.polygloat.dtos.response.RepositoryDTO;
import com.polygloat.helpers.JsonHelper;
import com.polygloat.model.Language;
import com.polygloat.model.Repository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static com.polygloat.controllers.LoggedRequestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RepositoryControllerTest extends SignedInControllerTest {
    private final LanguageDTO languageDTO = new LanguageDTO("English", "en");

    @Test
    void createRepository() throws Exception {
        dbPopulator.createBase("test");

        testCreateValidationSize();

        testCreateValidationUniqueness();

        testCreateCorrectRequest();
    }

    private void testCreateCorrectRequest() throws Exception {
        CreateRepositoryDTO request = new CreateRepositoryDTO("aaa", Collections.singleton(languageDTO));

        MvcResult mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Optional<Repository> aa = repositoryService.findByName("aaa", userAccount);

        assertThat(aa).isPresent();

        Repository repository = aa.orElse(null);

        assertThat(repository.getLanguages()).isNotEmpty();

        Language language = repository.getLanguages().stream().findFirst().orElse(null);

        assertThat(language).isNotNull();

        assertThat(language.getAbbreviation()).isEqualTo("en");

        assertThat(language.getName()).isEqualTo("English");
    }

    private void testCreateValidationSize() throws Exception {
        CreateRepositoryDTO request = new CreateRepositoryDTO("aa", Collections.singleton(languageDTO));

        //test validation
        MvcResult mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).contains("name");
        assertThat(mvcResult.getResponse().getContentAsString()).contains("STANDARD_VALIDATION");
    }

    private void testCreateValidationUniqueness() throws Exception {
        CreateRepositoryDTO request = new CreateRepositoryDTO("test", Collections.singleton(languageDTO));

        //test validation
        MvcResult mvcResult = mvc.perform(
                loggedPost("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo("{\"STANDARD_VALIDATION\":{\"name\":\"NAME_EXISTS\"}}");
    }


    @Test
    void editRepository() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString());
        Repository test2 = dbPopulator.createBase(generateUniqueString());

        testEditValidationUniqueness(test, test2.getName());
        testEditCorrectRequest(test);
    }

    private void testEditCorrectRequest(Repository test) throws Exception {
        AbstractRepositoryDTO request = new EditRepositoryDTO(test.getId(), "new test");

        MvcResult mvcResult = mvc.perform(
                loggedPost("/api/repositories/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(request)))
                .andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        RepositoryDTO response = mapper.readValue(mvcResult.getResponse().getContentAsString(), RepositoryDTO.class);

        assertThat(response.getName()).isEqualTo("new test");
        assertThat(response.getId()).isEqualTo(test.getId());

        Optional<Repository> found = repositoryService.findByName("new test", userAccount);

        assertThat(found).isPresent();
    }

    private void testEditValidationUniqueness(Repository repository, String nonUniqueName) throws Exception {
        EditRepositoryDTO request = new EditRepositoryDTO(repository.getId(), nonUniqueName);

        //test validation
        MvcResult mvcResult = mvc.perform(
                loggedPost("/api/repositories/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo("{\"STANDARD_VALIDATION\":{\"name\":\"NAME_EXISTS\"}}");
    }

    @Test
    @Rollback
    void deleteRepository() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString());

        MvcResult mvcResult = mvc.perform(
                loggedDelete("/api/repositories/" + test.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Optional<Repository> repository = repositoryService.findById(test.getId());

        assertThat(repository).isEmpty();
    }


    @Test
    void findAll() throws Exception {
        LinkedHashSet<String> repos = new LinkedHashSet<>();
        for (int i = 0; i < 3; i++) {
            repos.add(dbPopulator.createBase(generateUniqueString()).getName());
        }

        MvcResult mvcResult = mvc.perform(
                loggedGet("/api/repositories/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked") Set<RepositoryDTO> set = mapResponse(mvcResult, Set.class, RepositoryDTO.class);
        assertThat(set).extracting("name").containsAll(repos);
    }
}
