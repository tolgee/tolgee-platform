package io.polygloat.repository;

import io.polygloat.model.Permission;
import io.polygloat.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findOneByRepositoryIdAndUserId(Long repositoryId, Long userId);

    LinkedHashSet<Permission> getAllByRepositoryAndUserNotNull(io.polygloat.model.Repository repository);

    @Query("from Permission p join Repository r on r = p.repository where p.user = ?1 order by r.name")
    LinkedHashSet<Permission> findAllByUser(UserAccount userAccount);

    @Modifying
    @Query("delete from Permission p where p.repository.id = :repositoryId")
    void deleteAllByRepositoryId(Long repositoryId);
}
