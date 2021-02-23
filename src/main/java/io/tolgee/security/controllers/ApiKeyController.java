package io.tolgee.security.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.tolgee.constants.ApiScope;
import io.tolgee.constants.Message;
import io.tolgee.dtos.request.CreateApiKeyDTO;
import io.tolgee.dtos.request.EditApiKeyDTO;
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.exceptions.PermissionException;
import io.tolgee.model.ApiKey;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.service.ApiKeyService;
import io.tolgee.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/apiKeys")
public class ApiKeyController extends PrivateController {

    private final ApiKeyService apiKeyService;
    private final RepositoryService repositoryService;

    @Autowired
    public ApiKeyController(ApiKeyService apiKeyService, RepositoryService repositoryService) {
        this.apiKeyService = apiKeyService;
        this.repositoryService = repositoryService;
    }

    @Operation(summary = "Get all user's api keys")
    @GetMapping(path = "")
    public Set<ApiKeyDTO> allByUser() {
        return apiKeyService.getAllByUser(authenticationFacade.getUserAccount()).stream()
                .map(ApiKeyDTO::fromEntity)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @GetMapping(path = "/repository/{repositoryId}")
    public Set<ApiKeyDTO> allByRepository(@PathVariable("repositoryId") Long repositoryId) {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.MANAGE);
        return apiKeyService.getAllByRepository(repositoryId).stream()
                .map(ApiKeyDTO::fromEntity)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @PostMapping(path = "")
    public ApiKeyDTO create(@RequestBody() @Valid CreateApiKeyDTO createApiKeyDTO) {
        Repository repository = repositoryService.findById(createApiKeyDTO.getRepositoryId()).orElseThrow(() -> new NotFoundException(Message.REPOSITORY_NOT_FOUND));
        securityService.checkApiKeyScopes(createApiKeyDTO.getScopes(), repository);
        return apiKeyService.createApiKey(authenticationFacade.getUserAccount(), createApiKeyDTO.getScopes(), repository);
    }

    @PostMapping(path = "/edit")
    public void edit(@RequestBody() @Valid EditApiKeyDTO dto) {
        ApiKey apiKey = apiKeyService.getApiKey(dto.getId()).orElseThrow(() -> new NotFoundException(Message.API_KEY_NOT_FOUND));
        securityService.checkApiKeyScopes(dto.getScopes(), apiKey.getRepository());
        apiKey.setScopesSet(dto.getScopes());
        apiKeyService.editApiKey(apiKey);
    }

    @DeleteMapping(path = "/{key}")
    public void delete(@PathVariable("key") String key) {
        ApiKey apiKey = apiKeyService.getApiKey(key).orElseThrow(() -> new NotFoundException(Message.API_KEY_NOT_FOUND));
        try {
            securityService.checkRepositoryPermission(apiKey.getRepository().getId(), Permission.RepositoryPermissionType.MANAGE);
        } catch (PermissionException e) {
            //user can delete their own api keys
            if (!apiKey.getUserAccount().getId().equals(authenticationFacade.getUserAccount().getId())) {
                throw e;
            }
        }
        apiKeyService.deleteApiKey(apiKey);
    }

    @GetMapping(path = "/availableScopes")
    public Map<String, Set<String>> getScopes() {
        return Arrays.stream(Permission.RepositoryPermissionType.values())
                .collect(Collectors.toMap(Enum::name, type -> Arrays.stream(type.getAvailableScopes()).map(ApiScope::getValue).collect(Collectors.toSet())));
    }
}

