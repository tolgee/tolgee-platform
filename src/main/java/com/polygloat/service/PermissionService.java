package com.polygloat.service;

import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.PermissionRepository;
import com.polygloat.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final EntityManager entityManager;
    private final AuthenticationFacade authenticationFacade;

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
