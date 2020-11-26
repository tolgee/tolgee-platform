package io.polygloat.security.controllers;

import io.polygloat.dtos.response.InvitationDTO;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Invitation;
import io.polygloat.model.Permission;
import io.polygloat.model.Repository;
import io.polygloat.service.InvitationService;
import io.polygloat.service.RepositoryService;
import io.polygloat.service.SecurityService;
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

