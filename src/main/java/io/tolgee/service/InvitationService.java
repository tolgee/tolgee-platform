package io.tolgee.service;

import io.tolgee.constants.Message;
import io.tolgee.exceptions.BadRequestException;
import io.tolgee.model.Invitation;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import io.tolgee.repository.InvitationRepository;
import io.tolgee.repository.PermissionRepository;
import io.tolgee.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final PermissionRepository permissionRepository;
    private final EntityManager entityManager;
    private final AuthenticationFacade authenticationFacade;

    @Autowired
    public InvitationService(InvitationRepository invitationRepository, PermissionRepository permissionRepository, EntityManager entityManager, AuthenticationFacade authenticationFacade) {
        this.invitationRepository = invitationRepository;
        this.permissionRepository = permissionRepository;
        this.entityManager = entityManager;
        this.authenticationFacade = authenticationFacade;
    }

    @Transactional
    public String create(Repository repository, Permission.RepositoryPermissionType type) {
        String code = RandomStringUtils.randomAlphabetic(50);
        Invitation invitation = new Invitation(null, code);
        Permission permission = Permission.builder().invitation(invitation).repository(repository).type(type).build();
        invitation.setPermission(permission);
        permissionRepository.save(permission);
        invitationRepository.save(invitation);
        return code;
    }

    @Transactional
    public void removeExpired() {
        invitationRepository.deleteAllByCreatedAtLessThan(Date.from(Instant.now().minus(Duration.ofDays(30))));
    }

    @Transactional
    public void accept(String code) {
        this.accept(code, authenticationFacade.getUserAccount());
    }

    @Transactional
    public void accept(String code, UserAccount userAccount) {
        Invitation invitation = getInvitation(code);

        Permission permission = invitation.getPermission();

        if (this.permissionRepository.findOneByRepositoryIdAndUserId(permission.getRepository().getId(), userAccount.getId()) != null) {
            throw new BadRequestException(Message.USER_ALREADY_HAS_PERMISSIONS);
        }

        permission.setInvitation(null);
        permission.setUser(userAccount);
        permissionRepository.save(permission);

        //avoid cascade delete
        invitation.setPermission(null);
        invitationRepository.delete(invitation);
    }


    @NotNull
    public Invitation getInvitation(String code) {
        return invitationRepository.findOneByCode(code).orElseThrow(() ->
                //this exception is important for sign up service! Do not remove!!
                new BadRequestException(Message.INVITATION_CODE_DOES_NOT_EXIST_OR_EXPIRED));
    }

    public Optional<Invitation> findById(Long id) {
        return invitationRepository.findById(id);
    }

    public Set<Invitation> getForRepository(Repository repository) {
        return invitationRepository.findAllByPermissionRepositoryOrderByCreatedAt(repository);
    }

    @Transactional
    public void delete(Invitation invitation) {
        invitationRepository.delete(invitation);
    }
}
