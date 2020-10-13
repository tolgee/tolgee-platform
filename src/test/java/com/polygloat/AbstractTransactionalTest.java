package com.polygloat;

import com.polygloat.repository.LanguageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Transactional
public abstract class AbstractTransactionalTest extends AbstractTransactionalTestNGSpringContextTests {
    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected LanguageRepository languageRepository;

    protected void commitTransaction() {
        TestTransaction.flagForCommit();
        entityManager.flush();
        TestTransaction.end();
        TestTransaction.start();
        entityManager.clear();
    }

}
