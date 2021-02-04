package io.tolgee.repository;

import io.tolgee.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<io.tolgee.model.Repository, Long> {
    Optional<io.tolgee.model.Repository> findByNameAndCreatedBy(String name, UserAccount userAccount);

    LinkedHashSet<io.tolgee.model.Repository> findAllByCreatedBy(UserAccount userAccount);

    @Query("from Repository r join Permission p on p.repository = r where p.user = ?1 order by r.name")
    LinkedHashSet<io.tolgee.model.Repository> findAllPermitted(UserAccount userAccount);
}
