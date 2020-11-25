package io.polygloat.controllers;

import io.polygloat.dtos.PathDTO;
import io.polygloat.dtos.request.SetTranslationsDTO;
import io.polygloat.dtos.response.SourceResponseDTO;
import io.polygloat.dtos.response.ViewDataResponse;
import io.polygloat.dtos.response.translations_view.ResponseParams;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Permission;
import io.polygloat.model.Source;
import io.polygloat.service.RepositoryService;
import io.polygloat.service.SecurityService;
import io.polygloat.service.SourceService;
import io.polygloat.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/repository/{repositoryId}/translations")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TranslationController implements IController {

    private final TranslationService translationService;
    private final SourceService sourceService;
    private final RepositoryService repositoryService;
    private final SecurityService securityService;


    @GetMapping(value = "/{languages}")
    public Map<String, Object> getTranslations(@PathVariable("repositoryId") Long repositoryId,
                                               @PathVariable("languages") Set<String> languages) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW);
        return translationService.getTranslations(languages, repositoryId);
    }

    @PostMapping("/set")
    public void setTranslations(@PathVariable("repositoryId") Long repositoryId, @RequestBody @Valid SetTranslationsDTO dto) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE);
        Source source = sourceService.getSource(repositoryId, PathDTO.fromFullPath(dto.getKey())).orElseThrow(NotFoundException::new);
        translationService.setForSource(source, dto.getTranslations());
    }

    @GetMapping(value = "/view")
    public ViewDataResponse<LinkedHashSet<SourceResponseDTO>, ResponseParams> getViewData(@PathVariable("repositoryId") Long repositoryId,
                                                                                          @RequestParam(name = "languages", required = false) Set<String> languages,
                                                                                          @RequestParam(name = "limit", defaultValue = "10") int limit,
                                                                                          @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                                                          @RequestParam(name = "search", required = false) String search
    ) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.VIEW);
        return translationService.getViewData(languages, repositoryId, limit, offset, search);
    }
}
