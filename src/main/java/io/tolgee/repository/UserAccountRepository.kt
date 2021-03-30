package io.tolgee.repository;

import io.tolgee.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByThirdPartyAuthTypeAndThirdPartyAuthId(String thirdPartyAuthId, String thirdPartyAuthType);
}
