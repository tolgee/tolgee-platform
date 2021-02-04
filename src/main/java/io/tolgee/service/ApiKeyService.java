package io.tolgee.service;

import io.tolgee.constants.ApiScope;
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.ApiKey;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import io.tolgee.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PermissionService permissionService;
    private final SecureRandom random;

    @Autowired
    public ApiKeyService(ApiKeyRepository apiKeyRepository, PermissionService permissionService, SecureRandom random) {
        this.apiKeyRepository = apiKeyRepository;
        this.permissionService = permissionService;
        this.random = random;
    }

    public ApiKeyDTO createApiKey(UserAccount userAccount, Set<ApiScope> scopes, Repository repository) {
        ApiKey apiKey = ApiKey.builder()
                .key(new BigInteger(130, random).toString(32))
                .scopes(scopes)
                .repository(repository)
                .userAccount(userAccount).build();
        apiKeyRepository.save(apiKey);
        return ApiKeyDTO.fromEntity(apiKey);
    }

    public Set<ApiKey> getAllByUser(UserAccount userAccount) {
        return apiKeyRepository.getAllByUserAccountOrderById(userAccount);
    }

    public Set<ApiKey> getAllByRepository(Long repositoryId) {
        return apiKeyRepository.getAllByRepositoryId(repositoryId);
    }

    public Optional<ApiKey> getApiKey(String apiKey) {
        return apiKeyRepository.findByKey(apiKey);
    }

    public Optional<ApiKey> getApiKey(Long id) {
        return apiKeyRepository.findById(id);
    }

    public void deleteApiKey(ApiKey apiKey) {
        apiKeyRepository.delete(apiKey);
    }

    public Set<ApiScope> getAvailableScopes(UserAccount userAccount, Repository repository) {
        return Arrays.stream(
                permissionService.getRepositoryPermission(repository.getId(), userAccount).orElseThrow(NotFoundException::new).getType().getAvailableScopes()
        ).collect(Collectors.toSet());
    }

    public void editApiKey(ApiKey apiKey) {
        apiKeyRepository.save(apiKey);
    }

    public void deleteAllByRepository(Long repositoryId) {
        apiKeyRepository.deleteAllByRepositoryId(repositoryId);
    }
}
