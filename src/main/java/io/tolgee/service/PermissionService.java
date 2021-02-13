package io.tolgee.service;

import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import io.tolgee.repository.PermissionRepository;
import io.tolgee.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final EntityManager entityManager;
    private final AuthenticationFacade authenticationFacade;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository, EntityManager entityManager, AuthenticationFacade authenticationFacade) {
        this.permissionRepository = permissionRepository;
        this.entityManager = entityManager;
        this.authenticationFacade = authenticationFacade;
    }

    public Set<Permission> getAllOfRepository(Repository repository) {
        return this.permissionRepository.getAllByRepositoryAndUserNotNull(repository);
    }

    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id);
    }

    public Optional<Permission> getRepositoryPermission(Long repositoryId, UserAccount userAccount) {
        return this.permissionRepository.findOneByRepositoryIdAndUserId(repositoryId, userAccount.getId());
    }

    public void create(Permission permission) {
        permission.getRepository().getPermissions().add(permission);
        permissionRepository.save(permission);
    }

    public void delete(Permission permission) {
        permissionRepository.delete(permission);
    }

    public void deleteAllByRepository(Long repositoryId) {
        permissionRepository.deleteAllByRepositoryId(repositoryId);
    }

    @Transactional
    public void grantFullAccessToRepo(UserAccount userAccount, Repository repository) {
        Permission permission = Permission.builder().type(Permission.RepositoryPermissionType.MANAGE).repository(repository).user(userAccount).build();
        create(permission);
    }

    @Transactional
    public void editPermission(Permission permission, Permission.RepositoryPermissionType type) {
        permission.setType(type);
        permissionRepository.save(permission);
    }
}
