package com.polygloat.security.controllers;

import com.polygloat.constants.Message;
import com.polygloat.dtos.request.PermissionEditDto;
import com.polygloat.dtos.response.PermissionDTO;
import com.polygloat.exceptions.BadRequestException;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.security.AuthenticationFacade;
import com.polygloat.service.PermissionService;
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
@RequestMapping("/api/permission")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionController {

    private final SecurityService securityService;
    private final RepositoryService repositoryService;
    private final PermissionService permissionService;
    private final AuthenticationFacade authenticationFacade;

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

