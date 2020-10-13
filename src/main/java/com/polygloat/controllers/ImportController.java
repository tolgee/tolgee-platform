package com.polygloat.controllers;

import com.polygloat.constants.Message;
import com.polygloat.dtos.PathDTO;
import com.polygloat.dtos.request.ImportDto;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Language;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.model.Source;
import com.polygloat.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/repository/{repositoryId}/import")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ImportController implements IController {

    private final TranslationService translationService;
    private final SourceService sourceService;
    private final RepositoryService repositoryService;
    private final SecurityService securityService;
    private final LanguageService languageService;
    private final SmartValidator validator;

    @PostMapping(value = "")
    public void doImport(@PathVariable("repositoryId") Long repositoryId, @RequestBody ImportDto dto) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.MANAGE);
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(() -> new NotFoundException(Message.REPOSITORY_NOT_FOUND));

        Language language = languageService.getOrCreate(repository, dto.getLanguageAbbreviation());

        for (Map.Entry<String, String> entry : dto.getData().entrySet()) {
            Source source = sourceService.getOrCreateSource(repository, PathDTO.fromFullPath(entry.getKey()));
            translationService.setTranslation(source, language.getAbbreviation(), entry.getValue());
        }
    }
}
