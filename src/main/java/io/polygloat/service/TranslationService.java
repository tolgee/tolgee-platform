package io.polygloat.service;

import io.polygloat.constants.Message;
import io.polygloat.dtos.PathDTO;
import io.polygloat.dtos.query_results.KeyDTO;
import io.polygloat.dtos.response.KeyResponseDTO;
import io.polygloat.dtos.response.ViewDataResponse;
import io.polygloat.dtos.response.translations_view.ResponseParams;
import io.polygloat.exceptions.InternalException;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Language;
import io.polygloat.model.Repository;
import io.polygloat.model.Key;
import io.polygloat.model.Translation;
import io.polygloat.repository.TranslationRepository;
import io.polygloat.service.query_builders.TranslationsViewBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final EntityManager entityManager;

    @Setter(onMethod = @__({@Autowired}))
    private LanguageService languageService;

    @Setter(onMethod = @__({@Autowired}))
    private KeyService keyService;

    @Setter(onMethod = @__({@Autowired}))
    private RepositoryService repositoryService;

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
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(NotFoundException::new);
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

    public ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> getViewData(
            Set<String> languageAbbreviations, Long repositoryId, int limit, int offset, String search
    ) {
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(NotFoundException::new);

        Set<Language> languages = languageService.getLanguagesForTranslationsView(languageAbbreviations, repository);

        TranslationsViewBuilder.Result data = TranslationsViewBuilder.getData(entityManager, repository, languages, search, limit, offset);
        return new ViewDataResponse<>(data.getData()
                .stream()
                .map(queryResult -> KeyResponseDTO.fromQueryResult(new KeyDTO((Object[]) queryResult)))
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
}
