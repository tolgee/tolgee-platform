package com.polygloat.controllers;

import com.polygloat.constants.ApiScope;
import com.polygloat.constants.Message;
import com.polygloat.dtos.PathDTO;
import com.polygloat.dtos.request.SetTranslationsDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.ApiKey;
import com.polygloat.model.Language;
import com.polygloat.model.Repository;
import com.polygloat.model.Source;
import com.polygloat.security.AuthenticationFacade;
import com.polygloat.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/uaa")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserAppApiController implements IController {

    private final TranslationService translationService;
    private final SourceService sourceService;
    private final RepositoryService repositoryService;
    private final SecurityService securityService;
    private final AuthenticationFacade authenticationFacade;
    private final LanguageService languageService;

    @GetMapping(value = "/{languages}")
    public Map<String, Object> getTranslations(@PathVariable("languages") String languages) {
        ApiKey apiKey = authenticationFacade.getApiKey();
        securityService.checkApiKeyScopes(Set.of(ApiScope.TRANSLATIONS_VIEW), apiKey);
        return translationService.getTranslations(parseLanguages(languages).orElse(null), apiKey.getRepository().getId());
    }

    @GetMapping(value = "/source/{sourceFullPath}/{languages}")
    public Map<String, String> getSourceTranslations(@PathVariable("sourceFullPath") String fullPath,
                                                     @PathVariable("languages") String langs) {
        PathDTO pathDTO = PathDTO.fromFullPath(fullPath);
        ApiKey apiKey = authenticationFacade.getApiKey();
        securityService.checkApiKeyScopes(Set.of(ApiScope.TRANSLATIONS_VIEW), apiKey);
        return translationService.getSourceTranslationsResult(apiKey.getRepository().getId(), pathDTO, parseLanguages(langs).orElse(null));
    }

    @GetMapping(value = "/source/{sourceFullPath}")
    public Map<String, String> getSourceTranslations(@PathVariable("sourceFullPath") String fullPath) {
        PathDTO pathDTO = PathDTO.fromFullPath(fullPath);
        ApiKey apiKey = authenticationFacade.getApiKey();
        return translationService.getSourceTranslationsResult(apiKey.getRepository().getId(), pathDTO, null);
    }

    @PostMapping("")
    public void setTranslations(@RequestBody @Valid SetTranslationsDTO dto) {
        ApiKey apiKey = authenticationFacade.getApiKey();
        securityService.checkApiKeyScopes(Set.of(ApiScope.TRANSLATIONS_EDIT), apiKey);
        Repository repository = repositoryService.findById(apiKey.getRepository().getId()).orElseThrow(() -> new NotFoundException(Message.REPOSITORY_NOT_FOUND));
        Source source = sourceService.getOrCreateSource(repository, PathDTO.fromFullPath(dto.getSourceFullPath()));
        translationService.setForSource(source, dto.getTranslations());
    }

    @GetMapping("/languages")
    public Set<String> getLanguages() {
        ApiKey apiKey = authenticationFacade.getApiKey();
        securityService.checkApiKeyScopes(Set.of(ApiScope.TRANSLATIONS_EDIT), apiKey);
        return languageService.findAll(apiKey.getRepository().getId()).stream().map(Language::getAbbreviation).collect(Collectors.toSet());
    }

    @GetMapping("/scopes")
    public Set<String> getScopes() {
        ApiKey apiKey = authenticationFacade.getApiKey();
        return apiKey.getScopes().stream().map(ApiScope::getValue).collect(Collectors.toSet());
    }

    private Optional<Set<String>> parseLanguages(String languages) {
        if (languages == null) {
            return Optional.empty();
        }
        return Optional.of(new HashSet<>(Arrays.stream(
                languages.split(","))
                //filter out empty strings
                .filter(i ->
                        !i.isEmpty()
                )
                .collect(Collectors.toList())));
    }
}
