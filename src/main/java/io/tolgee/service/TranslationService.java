package io.tolgee.service;

import io.tolgee.constants.Message;
import io.tolgee.dtos.PathDTO;
import io.tolgee.dtos.query_results.KeyWithTranslationsDto;
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto;
import io.tolgee.dtos.response.ViewDataResponse;
import io.tolgee.dtos.response.translations_view.ResponseParams;
import io.tolgee.exceptions.InternalException;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Key;
import io.tolgee.model.Language;
import io.tolgee.model.Repository;
import io.tolgee.model.Translation;
import io.tolgee.repository.TranslationRepository;
import io.tolgee.service.query_builders.TranslationsViewBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final EntityManager entityManager;

    private LanguageService languageService;

    private KeyService keyService;

    private RepositoryService repositoryService;

    @Autowired
    public TranslationService(TranslationRepository translationRepository, EntityManager entityManager) {
        this.translationRepository = translationRepository;
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> getTranslations(Set<String> languageAbbreviations, Long repositoryId) {
        Set<Translation> allByLanguages = translationRepository.getTranslations(languageAbbreviations, repositoryId);

        HashMap<String, Object> langTranslations = new LinkedHashMap<>();
        for (Translation translation : allByLanguages) {
            Map<String, Object> map = (Map<String, Object>) langTranslations
                    .computeIfAbsent(translation.getLanguage().getAbbreviation(),
                            t -> new LinkedHashMap<>());
            addToMap(translation, map);
        }

        return langTranslations;
    }

    public Set<Translation> getAllByLanguageId(Long languageId){
        return translationRepository.getAllByLanguageId(languageId);
    }

    public Map<String, String> getKeyTranslationsResult(Long repositoryId, PathDTO path, Set<String> languageAbbreviations) {
        Repository repository = repositoryService.getById(repositoryId).orElseThrow(NotFoundException::new);
        Key key = keyService.get(repository, path).orElse(null);

        Set<Language> languages;
        if (languageAbbreviations == null) {
            languages = languageService.getImplicitLanguages(repository);
        } else {
            languages = languageService.findByAbbreviations(languageAbbreviations, repositoryId);
        }

        Set<Translation> translations = getKeyTranslations(languages, repository, key);

        Map<String, String> translationsMap = translations.stream().collect(Collectors.toMap(v -> v.getLanguage().getAbbreviation(), Translation::getText));

        for (Language language : languages) {
            if (translationsMap.keySet().stream().filter(l -> l.equals(language.getAbbreviation())).findAny().isEmpty()) {
                translationsMap.put(language.getAbbreviation(), "");
            }
        }

        return translationsMap;
    }

    private Set<Translation> getKeyTranslations(Set<Language> languages, Repository repository, Key key) {
        if (key != null) {
            return translationRepository.getTranslations(key, repository, languages);
        }
        return new LinkedHashSet<>();
    }

    public Translation getOrCreate(Key key, Language language) {
        return get(key, language).orElseGet(() -> Translation.builder().language(language).key(key).build());
    }

    public Optional<Translation> get(Key key, Language language) {
        return translationRepository.findOneByKeyAndLanguage(key, language);
    }

    public ViewDataResponse<LinkedHashSet<KeyWithTranslationsResponseDto>, ResponseParams> getViewData(
            Set<String> languageAbbreviations, Long repositoryId, int limit, int offset, String search
    ) {
        Repository repository = repositoryService.getById(repositoryId).orElseThrow(NotFoundException::new);

        Set<Language> languages = languageService.getLanguagesForTranslationsView(languageAbbreviations, repository);

        TranslationsViewBuilder.Result data = TranslationsViewBuilder.getData(entityManager, repository, languages, search, limit, offset);
        return new ViewDataResponse<>(data.getData()
                .stream()
                .map(queryResult -> KeyWithTranslationsResponseDto.fromQueryResult(new KeyWithTranslationsDto((Object[]) queryResult)))
                .collect(Collectors.toCollection(LinkedHashSet::new)), offset, data.getCount(),
                new ResponseParams(search, languages.stream().map(Language::getAbbreviation).collect(Collectors.toSet())));
    }

    public void setTranslation(Key key, String languageAbbreviation, String text) {
        Language language = languageService.findByAbbreviation(languageAbbreviation, key.getRepository())
                .orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        Translation translation = getOrCreate(key, language);
        translation.setText(text);
        translationRepository.save(translation);
    }

    public void setForKey(Key key, Map<String, String> translations) {
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                deleteIfExists(key, entry.getKey());
            }
            setTranslation(key, entry.getKey(), entry.getValue());
        }
    }

    public void deleteIfExists(Key key, String languageAbbreviation) {
        Language language = languageService.findByAbbreviation(languageAbbreviation, key.getRepository())
                .orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        translationRepository.findOneByKeyAndLanguage(key, language).ifPresent(translationRepository::delete);
    }


    public void deleteIfExists(Key key, Language language) {
        translationRepository.findOneByKeyAndLanguage(key, language).ifPresent(translationRepository::delete);
    }

    @SuppressWarnings("unchecked")
    private void addToMap(Translation translation, Map<String, Object> map) {
        for (String folderName : translation.getKey().getPath().getPath()) {
            Object childMap = map.computeIfAbsent(folderName, k -> new LinkedHashMap<>());
            if (childMap instanceof Map) {
                map = (Map<String, Object>) childMap;
                continue;
            }
            throw new InternalException(Message.DATA_CORRUPTED);
        }
        map.put(translation.getKey().getPath().getName(), translation.getText());
    }

    public void deleteAllByRepository(Long repositoryId) {
        translationRepository.deleteAllByRepositoryId(repositoryId);
    }

    public void deleteAllByLanguage(Long languageId) {
        translationRepository.deleteAllByLanguageId(languageId);
    }

    public void deleteAllByKeys(Collection<Long> ids) {
        translationRepository.deleteAllByKeyIds(ids);
    }

    public void deleteAllByKey(Long id) {
        translationRepository.deleteAllByKeyId(id);
    }

    @Autowired
    public void setLanguageService(LanguageService languageService) {
        this.languageService = languageService;
    }

    @Autowired
    public void setKeyService(KeyService keyService) {
        this.keyService = keyService;
    }

    @Autowired
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }
}
