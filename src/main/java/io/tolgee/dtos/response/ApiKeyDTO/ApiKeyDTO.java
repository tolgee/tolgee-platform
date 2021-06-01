package io.tolgee.dtos.response.ApiKeyDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import io.tolgee.constants.ApiScope;
import io.tolgee.model.ApiKey;

import java.util.Set;
import java.util.stream.Collectors;

@Schema
public class ApiKeyDTO {
    private Long id;

    @Schema(name = "Resulting user's api key")
    private String key;

    private String userName;

    private Long projectId;

    private String projectName;

    private Set<String> scopes;

    public ApiKeyDTO(Long id, String key, String userName, Long projectId, String projectName, Set<String> scopes) {
        this.id = id;
        this.key = key;
        this.userName = userName;
        this.projectId = projectId;
        this.projectName = projectName;
        this.scopes = scopes;
    }

    public ApiKeyDTO() {
    }

    public static ApiKeyDTO fromEntity(ApiKey apiKey) {
        return ApiKeyDTO.builder()
                .key(apiKey.getKey())
                .id(apiKey.getId())
                .userName(apiKey.getUserAccount().getName())
                .projectId(apiKey.getProject().getId())
                .projectName(apiKey.getProject().getName())
                .scopes(apiKey.getScopesEnum().stream().map(ApiScope::getValue).collect(Collectors.toSet()))
                .build();
    }

    public static ApiKeyDTOBuilder builder() {
        return new ApiKeyDTOBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public String getUserName() {
        return this.userName;
    }

    public Long getProjectId() {
        return this.projectId;
    }

    public String getProjectName() {
        return this.projectName;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiKeyDTO)) return false;
        final ApiKeyDTO other = (ApiKeyDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$userName = this.getUserName();
        final Object other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) return false;
        final Object this$projectId = this.getProjectId();
        final Object other$projectId = other.getProjectId();
        if (this$projectId == null ? other$projectId != null : !this$projectId.equals(other$projectId)) return false;
        final Object this$projectName = this.getProjectName();
        final Object other$projectName = other.getProjectName();
        if (this$projectName == null ? other$projectName != null : !this$projectName.equals(other$projectName)) return false;
        final Object this$scopes = this.getScopes();
        final Object other$scopes = other.getScopes();
        if (this$scopes == null ? other$scopes != null : !this$scopes.equals(other$scopes)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiKeyDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $userName = this.getUserName();
        result = result * PRIME + ($userName == null ? 43 : $userName.hashCode());
        final Object $projectId = this.getProjectId();
        result = result * PRIME + ($projectId == null ? 43 : $projectId.hashCode());
        final Object $projectName = this.getProjectName();
        result = result * PRIME + ($projectName == null ? 43 : $projectName.hashCode());
        final Object $scopes = this.getScopes();
        result = result * PRIME + ($scopes == null ? 43 : $scopes.hashCode());
        return result;
    }

    public String toString() {
        return "ApiKeyDTO(id=" + this.getId() + ", key=" + this.getKey() + ", userName=" + this.getUserName() + ", projectId=" + this.getProjectId() + ", projectName=" + this.getProjectName() + ", scopes=" + this.getScopes() + ")";
    }

    public static class ApiKeyDTOBuilder {
        private Long id;
        private String key;
        private String userName;
        private Long projectId;
        private String projectName;
        private Set<String> scopes;

        ApiKeyDTOBuilder() {
        }

        public ApiKeyDTO.ApiKeyDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApiKeyDTO.ApiKeyDTOBuilder key(String key) {
            this.key = key;
            return this;
        }

        public ApiKeyDTO.ApiKeyDTOBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public ApiKeyDTO.ApiKeyDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public ApiKeyDTO.ApiKeyDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public ApiKeyDTO.ApiKeyDTOBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public ApiKeyDTO build() {
            return new ApiKeyDTO(id, key, userName, projectId, projectName, scopes);
        }

        public String toString() {
            return "ApiKeyDTO.ApiKeyDTOBuilder(id=" + this.id + ", key=" + this.key + ", userName=" + this.userName + ", projectId=" + this.projectId + ", projectName=" + this.projectName + ", scopes=" + this.scopes + ")";
        }
    }
}
