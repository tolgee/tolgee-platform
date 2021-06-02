package io.tolgee.repository;

import io.tolgee.model.ApiKey;
import io.tolgee.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKey(@NotNull String key);

    LinkedHashSet<ApiKey> getAllByUserAccountOrderById(UserAccount userAccount);

    Set<ApiKey> getAllByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);
}

