package com.polygloat.repository;

import com.polygloat.model.ApiKey;
import com.polygloat.model.UserAccount;
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

    Set<ApiKey> getAllByRepositoryId(Long repositoryId);

    void deleteAllByRepositoryId(Long repositoryId);
}

