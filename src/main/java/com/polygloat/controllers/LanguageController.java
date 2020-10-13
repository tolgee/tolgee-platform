package com.polygloat.controllers;

import com.polygloat.constants.Message;
import com.polygloat.dtos.request.LanguageDTO;
import com.polygloat.dtos.request.validators.LanguageValidator;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Language;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.service.LanguageService;
import com.polygloat.service.RepositoryService;
import com.polygloat.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping("/api/repository/{repositoryId}/languages")
public class LanguageController implements IController {
    private final LanguageService languageService;
    private final RepositoryService repositoryService;
    private final LanguageValidator languageValidator;
    private final SecurityService securityService;

    @PostMapping(value = "")
    public LanguageDTO createLanguage(@PathVariable("repositoryId") Long repositoryId,
                                      @RequestBody @Valid LanguageDTO dto) {
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.MANAGE);
        languageValidator.validateCreate(dto, repository);
        Language language = languageService.createLanguage(dto, repository);
        return LanguageDTO.fromEntity(language);
    }

    @PostMapping(value = "/edit")
    public LanguageDTO editLanguage(@RequestBody @Valid LanguageDTO dto) {
        languageValidator.validateEdit(dto);
        Language language = languageService.findById(dto.getId()).orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        securityService.checkRepositoryPermission(language.getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        return LanguageDTO.fromEntity(languageService.editLanguage(dto));
    }

    @GetMapping(value = "")
    public Set<LanguageDTO> getAll(@PathVariable("repositoryId") Long repositoryId) {
        return languageService.findAll(repositoryId).stream().map(LanguageDTO::fromEntity)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @GetMapping(value = "{id}")
    public LanguageDTO get(@PathVariable("id") Long id) {
        Language language = languageService.findById(id).orElseThrow(NotFoundException::new);
        securityService.getAnyRepositoryPermission(language.getRepository().getId());
        return LanguageDTO.fromEntity(language);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteLanguage(@PathVariable Long id) {
        Language language = languageService.findById(id).orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        securityService.checkRepositoryPermission(language.getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        languageService.deleteLanguage(id);
    }
}
