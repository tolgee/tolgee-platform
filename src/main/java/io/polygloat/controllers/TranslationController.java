package io.polygloat.controllers;

import io.polygloat.dtos.PathDTO;
import io.polygloat.dtos.request.SetTranslationsDTO;
import io.polygloat.dtos.response.KeyResponseDTO;
import io.polygloat.dtos.response.ViewDataResponse;
import io.polygloat.dtos.response.translations_view.ResponseParams;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Key;
import io.polygloat.model.Permission;
import io.polygloat.service.KeyService;
import io.polygloat.service.RepositoryService;
import io.polygloat.service.SecurityService;
import io.polygloat.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/repository/{repositoryId}/translations")
public class TranslationController implements IController {

    private final TranslationService translationService;
    private final KeyService keyService;
    private final RepositoryService repositoryService;
    private final SecurityService securityService;

    @Autowired
    public TranslationController(TranslationService translationService, KeyService keyService, RepositoryService repositoryService, SecurityService securityService) {
        this.translationService = translationService;
        this.keyService = keyService;
        this.repositoryService = repositoryService;
        this.securityService = securityService;
    }


    @GetMapping(value = "/{languages}")
    public Map<String, Object> getTranslations(@PathVariable("repositoryId") Long repositoryId,
                                               @PathVariable("languages") Set<String> languages) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW);
        return translationService.getTranslations(languages, repositoryId);
    }

    @PostMapping("/set")
    public void setTranslations(@PathVariable("repositoryId") Long repositoryId, @RequestBody @Valid SetTranslationsDTO dto) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE);
        Key key = keyService.get(repositoryId, PathDTO.fromFullPath(dto.getKey())).orElseThrow(NotFoundException::new);
        translationService.setForKey(key, dto.getTranslations());
    }

    @GetMapping(value = "/view")
    public ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> getViewData(@PathVariable("repositoryId") Long repositoryId,
                                                                                       @RequestParam(name = "languages", required = false) Set<String> languages,
                                                                                       @RequestParam(name = "limit", defaultValue = "10") int limit,
                                                                                       @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                                                       @RequestParam(name = "search", required = false) String search
    ) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW);
        return translationService.getViewData(languages, repositoryId, limit, offset, search);
    }
}
