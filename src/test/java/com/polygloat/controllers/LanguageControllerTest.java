package com.polygloat.controllers;

import com.polygloat.dtos.request.LanguageDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.helpers.JsonHelper;
import com.polygloat.model.Language;
import com.polygloat.model.Repository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;

import static com.polygloat.controllers.LoggedRequestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LanguageControllerTest extends SignedInControllerTest implements ITest {

    private final LanguageDTO languageDTO = new LanguageDTO("en", "en");
    private final LanguageDTO languageDTOBlank = new LanguageDTO(null, "");
    private final LanguageDTO languageDTOCorrect = new LanguageDTO("Espanol", "es");

    @Test
    void createLanguage() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString());
        createLanguageTestValidation(test.getId());
        createLanguageCorrectRequest(test.getId());
    }

    @Test
    void editLanguage() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString());
        Language en = test.getLanguage("en").orElseThrow(NotFoundException::new);
        LanguageDTO languageDTO = LanguageDTO.fromEntity(en);

        languageDTO.setName("newEnglish");
        languageDTO.setAbbreviation("newEn");
        MvcResult mvcResult = performEdit(test.getId(), languageDTO)
                .andExpect(status().isOk()).andReturn();

        LanguageDTO languageDTORes = decodeJson(mvcResult.getResponse().getContentAsString(), LanguageDTO.class);
        assertThat(languageDTORes.getName()).isEqualTo(languageDTO.getName());
        assertThat(languageDTORes.getAbbreviation()).isEqualTo(languageDTO.getAbbreviation());

        Optional<Language> dbLanguage = languageService.findByAbbreviation(languageDTO.getAbbreviation(), test.getId());
        assertThat(dbLanguage).isPresent();
        assertThat(dbLanguage.get().getName()).isEqualTo(languageDTO.getName());
    }

    @Test
    void findAllLanguages() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString(), "ben");
        MvcResult mvcResult = performFindAll(test.getId()).andExpect(status().isOk()).andReturn();
        assertThat(decodeJson(mvcResult.getResponse().getContentAsString(), Set.class)).hasSize(2);
    }

    @Test
    void deleteLanguage() throws Exception {
        Repository test = dbPopulator.createBase(generateUniqueString());
        Language en = test.getLanguage("en").orElseThrow(NotFoundException::new);

        performDelete(test.getId(), en.getId()).andExpect(status().isOk());
        assertThat(languageService.findById(en.getId())).isEmpty();
        repositoryService.deleteRepository(test.getId());
    }

    private void createLanguageCorrectRequest(Long repoId) throws Exception {
        MvcResult mvcResult = performCreate(repoId, languageDTOCorrect).andExpect(status().isOk()).andReturn();
        LanguageDTO languageDTO = decodeJson(mvcResult.getResponse().getContentAsString(), LanguageDTO.class);

        assertThat(languageDTO.getName()).isEqualTo(languageDTOCorrect.getName());
        assertThat(languageDTO.getAbbreviation()).isEqualTo(languageDTOCorrect.getAbbreviation());

        Optional<Language> es = languageService.findByAbbreviation("es", repoId);
        assertThat(es).isPresent();
        assertThat(es.get().getName()).isEqualTo(languageDTOCorrect.getName());
    }


    void createLanguageTestValidation(Long repoId) throws Exception {
        MvcResult mvcResult = performCreate(repoId, languageDTO)
                .andExpect(status().isBadRequest()).andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).contains("language_abbreviation_exists");
        assertThat(mvcResult.getResponse().getContentAsString()).contains("language_name_exists");

        mvcResult = performCreate(repoId, languageDTOBlank)
                .andExpect(status().isBadRequest()).andReturn();


        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo("{\"STANDARD_VALIDATION\":" +
                        "{\"name\":\"must not be blank\"," +
                        "\"abbreviation\":\"must not be blank\"}}");
    }

    private ResultActions performCreate(Long repositoryId, LanguageDTO content) throws Exception {
        return mvc.perform(
                loggedPost("/api/repository/" + repositoryId + "/languages")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(content)));
    }

    private ResultActions performEdit(Long repositoryId, LanguageDTO content) throws Exception {
        return mvc.perform(
                loggedPost("/api/repository/" + repositoryId + "/languages/edit")
                        .contentType(MediaType.APPLICATION_JSON).content(
                        JsonHelper.asJsonString(content)));
    }

    private ResultActions performDelete(Long repositoryId, Long languageId) throws Exception {
        return mvc.perform(
                loggedDelete("/api/repository/" + repositoryId + "/languages/" + languageId)
                        .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions performFindAll(Long repositoryId) throws Exception {
        return mvc.perform(
                loggedGet("/api/repository/" + repositoryId + "/languages")
                        .contentType(MediaType.APPLICATION_JSON));
    }

}
