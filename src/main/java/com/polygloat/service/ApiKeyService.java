package com.polygloat.service;

import com.polygloat.constants.ApiScope;
import com.polygloat.dtos.response.ApiKeyDTO.ApiKeyDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.ApiKey;
import com.polygloat.model.Repository;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PermissionService permissionService;
    private final SecureRandom random;

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
