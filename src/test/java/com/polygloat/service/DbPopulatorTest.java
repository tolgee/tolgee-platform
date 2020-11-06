package com.polygloat.service;

import com.polygloat.development.DbPopulatorReal;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.ApiKey;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.RepositoryRepository;
import com.polygloat.repository.UserAccountRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

    @Value("${app.initialUsername:admin}")
    String initialUsername;

    @Autowired
    EntityManager entityManager;

    private UserAccount userAccount;

    @BeforeMethod
    public void setup() {
        populator.autoPopulate();
        userAccount = userAccountRepository.findByUsername(initialUsername).orElseThrow(NotFoundException::new);
    }

    @Test
    @Transactional
    void createsUser() {
        assertThat(userAccount.getName()).isEqualTo(initialUsername);
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
