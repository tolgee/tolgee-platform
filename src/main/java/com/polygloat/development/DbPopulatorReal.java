package com.polygloat.development;

import com.polygloat.dtos.request.LanguageDTO;
import com.polygloat.dtos.request.SignUpDto;
import com.polygloat.model.*;
import com.polygloat.repository.RepositoryRepository;
import com.polygloat.repository.UserAccountRepository;
import com.polygloat.service.LanguageService;
import com.polygloat.service.PermissionService;
import com.polygloat.service.SecurityService;
import com.polygloat.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DbPopulatorReal {
    private final EntityManager entityManager;
    private final UserAccountRepository userAccountRepository;
    private final PermissionService permissionService;
    private final UserAccountService userAccountService;
    private final SecurityService securityService;
    public static final String DEFAULT_USERNAME = "ben";
    private final LanguageService languageService;
    private final RepositoryRepository repositoryRepository;

    private Language de;
    private Language en;

    @Transactional
    public void autoPopulate() {
        //do not populate if db is not empty
        if (userAccountRepository.count() == 0) {
            this.populate("Application");
        }
    }

    public UserAccount createUser(String username) {
        return userAccountService.getByUserName(username).orElseGet(() -> {
            SignUpDto signUpDto = new SignUpDto();
            signUpDto.setEmail(username);
            signUpDto.setName(username);
            signUpDto.setPassword(username);
            return userAccountService.createUser(signUpDto);
        });
    }

    @Transactional
    public Repository createBase(String repositoryName, String username) {

        UserAccount userAccount = createUser(username);

        Repository repository = new Repository();
        repository.setName(repositoryName);
        repository.setCreatedBy(userAccount);

        en = createLanguage("en", repository);
        de = createLanguage("de", repository);

        permissionService.grantFullAccessToRepo(userAccount, repository);

        repositoryRepository.saveAndFlush(repository);
        entityManager.flush();
        entityManager.clear();

        return repository;
    }


    @Transactional
    public Repository createBase(String repositoryName) {
        return createBase(repositoryName, DEFAULT_USERNAME);
    }


    @Transactional
    public Repository populate(String repositoryName) {
        return populate(repositoryName, DEFAULT_USERNAME);
    }

    @Transactional
    public Repository populate(String repositoryName, String userName) {
        Repository repository = createBase(repositoryName, userName);

        createTranslation(repository, "Hello world!", "Hallo Welt!", en, de);
        createTranslation(repository, "English text one.", "Deutsch text einz.", en, de);

        createTranslation(repository, "This is translation in home folder",
                "Das ist die Übersetzung im Home-Ordner", en, de);

        createTranslation(repository, "This is translation in news folder",
                "Das ist die Übersetzung im News-Ordner", en, de);
        createTranslation(repository, "This is another translation in news folder",
                "Das ist eine weitere Übersetzung im Nachrichtenordner", en, de);

        createTranslation(repository, "This is standard text somewhere in DOM.",
                "Das ist Standardtext irgendwo im DOM.", en, de);
        createTranslation(repository, "This is another standard text somewhere in DOM.",
                "Das ist ein weiterer Standardtext irgendwo in DOM.", en, de);
        createTranslation(repository, "This is translation retrieved by service.",
                "Dase Übersetzung wird vom Service abgerufen.", en, de);
        createTranslation(repository, "This is textarea with placeholder and value.",
                "Das ist ein Textarea mit Placeholder und Value.", en, de);
        createTranslation(repository, "This is textarea with placeholder.",
                "Das ist ein Textarea mit Placeholder.", en, de);
        createTranslation(repository, "This is input with value.",
                "Das ist ein Input mit value.", en, de);
        createTranslation(repository, "This is input with placeholder.",
                "Das ist ein Input mit Placeholder.", en, de);

        return repository;
    }


    private Language createLanguage(String name, Repository repository) {
        return languageService.createLanguage(LanguageDTO.builder().name(name).abbreviation(name).build(), repository);
    }

    private void createTranslation(Repository repository, String english,
                                   String deutsch, Language en, Language de) {


        Source source = Source.builder().name("sampleApp." + english.replace(" ", "_").toLowerCase().replaceAll("\\.+$", ""))
                .repository(repository).build();

        Translation translation = new Translation();
        translation.setLanguage(en);
        translation.setSource(source);
        translation.setText(english);

        source.getTranslations().add(translation);
        source.getTranslations().add(translation);

        entityManager.persist(translation);

        Translation translationDe = new Translation();
        translationDe.setLanguage(de);
        translationDe.setSource(source);
        translationDe.setText(deutsch);

        source.getTranslations().add(translationDe);


        entityManager.persist(translationDe);

        entityManager.persist(source);
        entityManager.flush();
    }


}
