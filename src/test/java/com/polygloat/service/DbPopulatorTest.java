package com.polygloat.service;

import com.polygloat.development.DbPopulatorReal;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.RepositoryRepository;
import com.polygloat.repository.UserAccountRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

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
    EntityManager entityManager;

    @Test
    @Transactional
    void testPopulator() {
        populator.autoPopulate();

        List<UserAccount> all = userAccountRepository.findAll();

        assertThat(all.size()).isGreaterThan(0);

        entityManager.refresh(all.get(0));

        Hibernate.initialize(all.get(0).getCreatedRepositories());

        assertThat(all.get(0).getCreatedRepositories().size()).isGreaterThan(0);
    }

}
