package com.polygloat.repository;

import com.polygloat.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    void deleteAllByCreatedAtLessThan(Date date);

    Optional<Invitation> findOneByCode(String code);

    LinkedHashSet<Invitation> findAllByPermissionRepositoryOrderByCreatedAt(com.polygloat.model.Repository repository);
}

