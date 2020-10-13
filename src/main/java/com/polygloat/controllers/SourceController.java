package com.polygloat.controllers;

import com.polygloat.constants.Message;
import com.polygloat.dtos.request.EditSourceDTO;
import com.polygloat.dtos.request.SetTranslationsDTO;
import com.polygloat.dtos.response.SourceDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Permission;
import com.polygloat.model.Source;
import com.polygloat.service.SecurityService;
import com.polygloat.service.SourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping("/api/repository/{repositoryId}/sources")
public class SourceController implements IController {

    private final SourceService sourceService;
    private final SecurityService securityService;

    @PostMapping("/create")
    public void create(@PathVariable("repositoryId") Long repositoryId, @RequestBody @Valid SetTranslationsDTO dto) {
        Permission permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.TRANSLATE);
        sourceService.createSource(permission.getRepository(), dto);
    }

    @PostMapping(value = "/edit")
    public void edit(@PathVariable("repositoryId") Long repositoryId, @RequestBody @Valid EditSourceDTO dto) {
        Permission permission = securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT);
        sourceService.editSource(permission.getRepository(), dto);
    }

    @GetMapping(value = "{id}")
    public SourceDTO get(@PathVariable("id") Long id) {
        Source source = sourceService.getSource(id).orElseThrow(NotFoundException::new);
        securityService.getAnyRepositoryPermission(source.getRepository().getId());
        return SourceDTO.builder().fullPathString(source.getName()).build();
    }

    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable Long id) {
        Source source = sourceService.getSource(id).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(source.getRepository().getId(), Permission.RepositoryPermissionType.EDIT);
        sourceService.deleteSource(id);
    }

    @DeleteMapping(value = "")
    @Transactional
    public void delete(@PathVariable("repositoryId") Long repositoryId, @RequestBody Set<Long> ids) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.EDIT);
        for (Source source : sourceService.getSources(ids)) {
            if (!repositoryId.equals(source.getRepository().getId())) {
                throw new ValidationException(Message.SOURCE_NOT_FROM_REPOSITORY.getCode());
            }
            sourceService.deleteSources(ids);
        }
    }
}
