package com.polygloat.development;

import org.springframework.stereotype.Component;

@Component
public class DbPopulator {
  /*  private UserRepository userRepository;

    private RepositoryRepository repositoryRepository;
    private LanguageRepository languageRepository;
    private SourceRepository sourceRepository;
    private TranslationRepository translationRepository;
    private EntityManager entityManager;

    private Faker faker = new Faker();

    @Autowired
    public DbPopulator(UserRepository userRepository,
                       RepositoryRepository repositoryRepository,
                       LanguageRepository languageRepository,
                       SourceRepository sourceRepository,
                       TranslationRepository translationRepository,
                       EntityManager entityManager) {
        this.userRepository = userRepository;
        this.repositoryRepository = repositoryRepository;
        this.languageRepository = languageRepository;
        this.sourceRepository = sourceRepository;
        this.translationRepository = translationRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void populate() {
        int count = 0;
        System.out.println();
        for (int i = 0; i < 10; i++) {
            UserAccount user = fakerUser();
            //userRepository.save(user);
            entityManager.persist(user);
            System.out.print(String.format("\rCreating user %s.", user.getUsername()));

            for (int j = 0; j < 10; j++) {
                Repository repository = fakeRepository(user);
                System.out.print(String.format("\rCreating reposiroty %s.", repository.getName()));

                entityManager.persist(repository);

                //repositoryRepository.save(repository);
                Language en = fakeLanguage(repository, "English", "en");
                Language de = fakeLanguage(repository, "Deutsch", "de");

                for (Language lang : new Language[]{en, de}) {
                    System.out.print(String.format("\rCreating language %s.", lang.getName()));
                    //languageRepository.save(lang);
                    entityManager.persist(lang);
                }
                for (int z = 0; z < 50; z++) {
                    Source source = fakeSource(repository);
                    System.out.print(String.format("\rCreating source %s.", source.getFile().getName()));
                    //sourceRepository.save(source);
                    entityManager.persist(source);

                    for (Language lang : new Language[]{en, de}) {
                        Translation translation = fakeTranslation(source, lang);
                        System.out.print(String.format("\rCreating translation %s in lang %s. Already created %d translations.",
                                translation.getText(), lang.getName(), count));
                        entityManager.persist(translation);
                        //translationRepository.save(translation);
                        count++;
                    }
                }
            }
        }
        entityManager.flush();
    }

    private UserAccount fakerUser() {
        return new UserAccount(faker.harryPotter().character());
    }

    private Repository fakeRepository(UserAccount userAccount) {
        return new Repository(userAccount, faker.book().title(), faker.lorem().sentence(10, 10));
    }

    private Language fakeLanguage(Repository repository, String name, String abbr) {
        Language language = new Language();
        language.setName(name);
        language.setAbbreviation(abbr);
        language.setRepository(repository);
        return language;
    }

    //Todo: create folder structure
    private File fakeFolder(Repository repository) {
        File folder = new File();
        folder.setName(faker.lorem().word());
        return folder;
    }

    private Source fakeSource(Repository repository) {
        Source source = new Source();
        source.setRepository(repository);
        source.(faker.lebowski().quote().replace(" ", "_"));
        return source;
    }

    private Translation fakeTranslation(Source source, Language language) {
        Translation translation = new Translation();
        translation.setLanguage(language);
        translation.setSource(source);
        Faker faker = new Faker(new Locale(language.getAbbreviation()));
        translation.setText(faker.weather().description());
        return translation;
    }*/
}
