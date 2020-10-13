package com.polygloat.security.controllers;

import com.polygloat.dtos.response.InvitationDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Invitation;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.service.InvitationService;
import com.polygloat.service.RepositoryService;
import com.polygloat.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitation")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InvitationController {

    private final InvitationService invitationService;
    private final SecurityService securityService;
    private final RepositoryService repositoryService;

    @GetMapping("/accept/{code}")
    public void acceptInvitation(@PathVariable("code") String code) {
        invitationService.removeExpired();
        invitationService.accept(code);
    }

    @GetMapping("/list/{repositoryId}")
    public Set<InvitationDTO> getRepositoryInvitations(@PathVariable("repositoryId") Long id) {
        Repository repository = repositoryService.findById(id).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE);
        return invitationService.getForRepository(repository).stream().map(InvitationDTO::fromEntity).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @DeleteMapping("/{invitationId}")
    public void deleteInvitation(@PathVariable("invitationId") Long id) {
        Invitation invitation = invitationService.findById(id).orElseThrow(NotFoundException::new);
        securityService.checkRepositoryPermission(invitation.getPermission().getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        invitationService.delete(invitation);
    }
}

