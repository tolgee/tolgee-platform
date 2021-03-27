package io.tolgee.service;

import io.tolgee.configuration.tolgee.TolgeeProperties;
import io.tolgee.development.DbPopulatorReal;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.ApiKey;
import io.tolgee.model.UserAccount;
import io.tolgee.repository.RepositoryRepository;
import io.tolgee.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DbPopulatorTest extends AbstractTransactionalTestNGSpringContextTests {

    @Autowired
    DbPopulatorReal populator;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ApiKeyService apiKeyService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TolgeeProperties tolgeeProperties;


    private UserAccount userAccount;

    @BeforeMethod
    public void setup() {
        populator.autoPopulate();
        userAccount = userAccountRepository.findByUsername(tolgeeProperties.getAuthentication().getInitialUsername()).orElseThrow(NotFoundException::new);
    }

    @Test
    @Transactional
    void createsUser() {
        assertThat(userAccount.getName()).isEqualTo(tolgeeProperties.getAuthentication().getInitialUsername());
    }

    @Test
    @Transactional
    void createsRepository() {
        entityManager.refresh(userAccount);
        assertThat(userAccount.getCreatedRepositories().size()).isPositive();
    }

    @Test
    @Transactional
    void createsApiKey() {
        Optional<ApiKey> key = apiKeyService.getAllByUser(userAccount).stream().findFirst();
        assertThat(key).isPresent();
        ApiKey apiKey = key.get();
        assertThat(apiKey.getKey()).isEqualTo("this_is_dummy_api_key");
    }
}
