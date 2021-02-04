package io.tolgee.security.controllers;

import io.tolgee.constants.Message;
import io.tolgee.dtos.request.PermissionEditDto;
import io.tolgee.dtos.response.PermissionDTO;
import io.tolgee.exceptions.BadRequestException;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.PermissionService;
import io.tolgee.service.RepositoryService;
import io.tolgee.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final SecurityService securityService;
    private final RepositoryService repositoryService;
    private final PermissionService permissionService;
    private final AuthenticationFacade authenticationFacade;

    @Autowired
    public PermissionController(SecurityService securityService, RepositoryService repositoryService, PermissionService permissionService, AuthenticationFacade authenticationFacade) {
        this.securityService = securityService;
        this.repositoryService = repositoryService;
        this.permissionService = permissionService;
        this.authenticationFacade = authenticationFacade;
    }

    @GetMapping("/list/{repositoryId}")
    public Set<PermissionDTO> getRepositoryPermissions(@PathVariable("repositoryId") Long id) {
        Repository repository = repositoryService.findById(id).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE);
        return permissionService.getAllOfRepository(repository).stream().map(PermissionDTO::fromEntity).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @DeleteMapping("/{permissionId}")
    public void deletePermission(@PathVariable("permissionId") Long id) {
        Permission permission = permissionService.findById(id).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(permission.getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        if (permission.getUser().getId().equals(authenticationFacade.getUserAccount().getId())) {
            throw new BadRequestException(Message.CAN_NOT_REVOKE_OWN_PERMISSIONS);
        }
        permissionService.delete(permission);
    }

    @PostMapping("edit")
    public void editPermission(@RequestBody @Valid PermissionEditDto dto) {
        Permission permission = permissionService.findById(dto.getPermissionId()).orElseThrow(
                () -> new NotFoundException(Message.PERMISSION_NOT_FOUND));

        securityService.checkRepositoryPermission(permission.getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        permissionService.editPermission(permission, dto.getType());
    }
}

