package io.polygloat.repository;

import io.polygloat.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<io.polygloat.model.Repository, Long> {
    Optional<io.polygloat.model.Repository> findByNameAndCreatedBy(String name, UserAccount userAccount);

    LinkedHashSet<io.polygloat.model.Repository> findAllByCreatedBy(UserAccount userAccount);

    @Query("from Repository r join Permission p on p.repository = r where p.user = ?1 order by r.name")
    LinkedHashSet<io.polygloat.model.Repository> findAllPermitted(UserAccount userAccount);
}
