package io.polygloat.controllers;

import io.polygloat.dtos.request.CreateRepositoryDTO;
import io.polygloat.dtos.request.EditRepositoryDTO;
import io.polygloat.dtos.request.InviteUser;
import io.polygloat.dtos.response.RepositoryDTO;
import io.polygloat.exceptions.InvalidStateException;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Permission;
import io.polygloat.model.Repository;
import io.polygloat.model.UserAccount;
import io.polygloat.security.AuthenticationFacade;
import io.polygloat.service.InvitationService;
import io.polygloat.service.RepositoryService;
import io.polygloat.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@RestController("_repositoryController")
@CrossOrigin(origins = "*")
@RequestMapping("/api/repositories")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RepositoryController implements IController {

    private final RepositoryService repositoryService;
    private final AuthenticationFacade authenticationFacade;
    private final SecurityService securityService;
    private final InvitationService invitationService;

    @PostMapping(value = "")
    public RepositoryDTO createRepository(@RequestBody @Valid CreateRepositoryDTO dto) {
        UserAccount userAccount = authenticationFacade.getUserAccount();
        Repository repository = repositoryService.createRepository(dto, userAccount);
        return RepositoryDTO.fromEntityAndPermission(repository, repository.getPermissions().stream().findAny().orElseThrow(InvalidStateException::new));
    }

    @GetMapping(value = "/{id}")
    public RepositoryDTO getRepository(@PathVariable("id") Long id) {
        Permission permission = securityService.getAnyRepositoryPermission(id);
        return RepositoryDTO.fromEntityAndPermission(repositoryService.findById(id).orElseThrow(null), permission);
    }


    @PostMapping(value = "/edit")
    public RepositoryDTO editRepository(@RequestBody @Valid EditRepositoryDTO dto) {
        Permission permission = securityService.checkRepositoryPermission(dto.getRepositoryId(), Permission.RepositoryPermissionType.MANAGE);
        Repository repository = repositoryService.editRepository(dto);
        return RepositoryDTO.fromEntityAndPermission(repository, permission);
    }

    @GetMapping(value = "")
    public Set<RepositoryDTO> getAll() {
        return repositoryService.findAllPermitted(authenticationFacade.getUserAccount());
    }

    @DeleteMapping(value = "/{id}")
    public void deleteRepository(@PathVariable Long id) {
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE);
        repositoryService.deleteRepository(id);
    }

    @PostMapping("/invite")
    public String inviteUser(@RequestBody InviteUser invitation) {
        securityService.checkRepositoryPermission(invitation.getRepositoryId(), Permission.RepositoryPermissionType.MANAGE);
        Repository repository = repositoryService.findById(invitation.getRepositoryId()).orElseThrow(NotFoundException::new);
        return invitationService.create(repository, invitation.getType());
    }
}
