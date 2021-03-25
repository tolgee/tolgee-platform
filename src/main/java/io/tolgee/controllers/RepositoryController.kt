package io.tolgee.controllers;

import io.tolgee.dtos.request.CreateRepositoryDTO;
import io.tolgee.dtos.request.EditRepositoryDTO;
import io.tolgee.dtos.request.InviteUser;
import io.tolgee.dtos.response.RepositoryDTO;
import io.tolgee.exceptions.InvalidStateException;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.InvitationService;
import io.tolgee.service.RepositoryService;
import io.tolgee.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@RestController("_repositoryController")
@CrossOrigin(origins = "*")
@RequestMapping("/api/repositories")
public class RepositoryController implements IController {

    private final RepositoryService repositoryService;
    private final AuthenticationFacade authenticationFacade;
    private final SecurityService securityService;
    private final InvitationService invitationService;

    @Autowired
    public RepositoryController(RepositoryService repositoryService, AuthenticationFacade authenticationFacade, SecurityService securityService, InvitationService invitationService) {
        this.repositoryService = repositoryService;
        this.authenticationFacade = authenticationFacade;
        this.securityService = securityService;
        this.invitationService = invitationService;
    }

    @PostMapping(value = "")
    public RepositoryDTO createRepository(@RequestBody @Valid CreateRepositoryDTO dto) {
        UserAccount userAccount = authenticationFacade.getUserAccount();
        Repository repository = repositoryService.createRepository(dto, userAccount);
        return RepositoryDTO.fromEntityAndPermission(repository, repository.getPermissions().stream().findAny().orElseThrow(InvalidStateException::new));
    }

    @GetMapping(value = "/{id}")
    public RepositoryDTO getRepository(@PathVariable("id") Long id) {
        Permission permission = securityService.getAnyRepositoryPermission(id);
        return RepositoryDTO.fromEntityAndPermission(repositoryService.getById(id).orElseThrow(null), permission);
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
        Repository repository = repositoryService.getById(invitation.getRepositoryId()).orElseThrow(NotFoundException::new);
        return invitationService.create(repository, invitation.getType());
    }
}
