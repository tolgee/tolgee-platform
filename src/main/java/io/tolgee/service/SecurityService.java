package io.tolgee.service;

import io.tolgee.constants.ApiScope;
import io.tolgee.exceptions.PermissionException;
import io.tolgee.model.ApiKey;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import io.tolgee.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class SecurityService {
    private final AuthenticationFacade authenticationFacade;
    private final PermissionService permissionService;
    private final ApiKeyService apiKeyService;

    @Autowired
    public SecurityService(AuthenticationFacade authenticationFacade, PermissionService permissionService, ApiKeyService apiKeyService) {
        this.authenticationFacade = authenticationFacade;
        this.permissionService = permissionService;
        this.apiKeyService = apiKeyService;
    }

    @Transactional
    public void grantFullAccessToRepo(Repository repository) {
        permissionService.grantFullAccessToRepo(getActiveUser(), repository);
    }

    private Optional<Permission> getRepositoryPermission(Long repositoryId) {
        return permissionService.getRepositoryPermission(repositoryId, getActiveUser());
    }

    public Permission getAnyRepositoryPermissionOrThrow(Long repositoryId) {
        Optional<Permission> repositoryPermission = getRepositoryPermission(repositoryId);
        if (repositoryPermission.isEmpty()) {
            throw new PermissionException();
        }

        return repositoryPermission.get();
    }


    public Permission checkRepositoryPermission(Long repositoryId, Permission.RepositoryPermissionType requiredPermission) {
        Permission usersPermission = getAnyRepositoryPermissionOrThrow(repositoryId);
        if (requiredPermission.getPower() > usersPermission.getType().getPower()) {
            throw new PermissionException();
        }
        return usersPermission;
    }


    public void checkApiKeyScopes(Set<ApiScope> scopes, Repository repository) {
        if (!apiKeyService.getAvailableScopes(getActiveUser(), repository).containsAll(scopes)) {
            throw new PermissionException();
        }
    }

    public void checkApiKeyScopes(Set<ApiScope> scopes, ApiKey apiKey) {
        checkApiKeyScopes(scopes, apiKey.getRepository()); // checks if user's has permissions to use api key its permissions
        if (!apiKey.getScopesEnum().containsAll(scopes)) {
            throw new PermissionException();
        }
    }

    private UserAccount getActiveUser() {
        return this.authenticationFacade.getUserAccount();
    }

}
