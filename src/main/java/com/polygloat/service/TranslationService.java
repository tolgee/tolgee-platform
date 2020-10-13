package com.polygloat.service;

import com.polygloat.constants.Message;
import com.polygloat.dtos.PathDTO;
import com.polygloat.dtos.query_results.SourceDTO;
import com.polygloat.dtos.response.SourceResponseDTO;
import com.polygloat.dtos.response.ViewDataResponse;
import com.polygloat.dtos.response.translations_view.ResponseParams;
import com.polygloat.exceptions.InternalException;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Language;
import com.polygloat.model.Repository;
import com.polygloat.model.Source;
import com.polygloat.model.Translation;
import com.polygloat.repository.TranslationRepository;
import com.polygloat.service.query_builders.TranslationsViewBuilder;
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
    private SourceService sourceService;

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

    public Map<String, String> getSourceTranslationsResult(Long repositoryId, PathDTO path, Set<String> languageAbbreviations) {
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(NotFoundException::new);
        Source source = sourceService.getSource(repository, path).orElse(null);


        Set<Language> languages;
        if (languageAbbreviations == null) {
            languages = languageService.getImplicitLanguages(repository);
        } else {
            languages = languageService.findByAbbreviations(languageAbbreviations, repositoryId);
        }

        Set<Translation> translations = getSourceTranslations(languages, repository, source);

        Map<String, String> translationsMap = translations.stream().collect(Collectors.toMap(v -> v.getLanguage().getAbbreviation(), Translation::getText));

        for (Language language : languages) {
            if (translationsMap.keySet().stream().filter(l -> l.equals(language.getAbbreviation())).findAny().isEmpty()) {
                translationsMap.put(language.getAbbreviation(), "");
            }
        }

        return translationsMap;
    }

    private Set<Translation> getSourceTranslations(Set<Language> languages, Repository repository, Source source) {
        if (source != null) {
            return translationRepository.getTranslations(source, repository, languages);
        }
        return new LinkedHashSet<>();
    }

    public Translation getOrCreate(Source source, Language language) {
        return get(source, language).orElseGet(() -> Translation.builder().language(language).source(source).build());
    }

    public Optional<Translation> get(Source source, Language language) {
        return translationRepository.findOneBySourceAndLanguage(source, language);
    }

    public ViewDataResponse<LinkedHashSet<SourceResponseDTO>, ResponseParams> getViewData(
            Set<String> languageAbbreviations, Long repositoryId, int limit, int offset, String search
    ) {
        Repository repository = repositoryService.findById(repositoryId).orElseThrow(NotFoundException::new);

        Set<Language> languages = languageService.getLanguagesForTranslationsView(languageAbbreviations, repository);

        TranslationsViewBuilder.Result data = TranslationsViewBuilder.getData(entityManager, repository, languages, search, limit, offset);
        return new ViewDataResponse<>(data.getData()
                .stream()
                .map(queryResult -> SourceResponseDTO.fromQueryResult(new SourceDTO((Object[]) queryResult)))
                .collect(Collectors.toCollection(LinkedHashSet::new)), offset, data.getCount(),
                new ResponseParams(search, languages.stream().map(Language::getAbbreviation).collect(Collectors.toSet())));
    }

    public void setTranslation(Source source, String languageAbbreviation, String text) {
        Language language = languageService.findByAbbreviation(languageAbbreviation, source.getRepository())
                .orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        Translation translation = getOrCreate(source, language);
        translation.setText(text);
        translationRepository.save(translation);
    }

    public void setForSource(Source source, Map<String, String> translations) {
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                deleteIfExists(source, entry.getKey());
            }
            setTranslation(source, entry.getKey(), entry.getValue());
        }
    }

    public void deleteIfExists(Source source, String languageAbbreviation) {
        Language language = languageService.findByAbbreviation(languageAbbreviation, source.getRepository())
                .orElseThrow(() -> new NotFoundException(Message.LANGUAGE_NOT_FOUND));
        translationRepository.findOneBySourceAndLanguage(source, language).ifPresent(translationRepository::delete);
    }


    public void deleteIfExists(Source source, Language language) {
        translationRepository.findOneBySourceAndLanguage(source, language).ifPresent(translationRepository::delete);
    }

    @SuppressWarnings("unchecked")
    private void addToMap(Translation translation, Map<String, Object> map) {
        for (String folderName : translation.getSource().getPath().getPath()) {
            Object childMap = map.computeIfAbsent(folderName, k -> new LinkedHashMap<>());
            if (childMap instanceof Map) {
                map = (Map<String, Object>) childMap;
                continue;
            }
            throw new InternalException(Message.DATA_CORRUPTED);
        }
        map.put(translation.getSource().getPath().getName(), translation.getText());
    }

    public void deleteAllByRepository(Long repositoryId) {
        translationRepository.deleteAllByRepositoryId(repositoryId);
    }

    public void deleteAllByLanguage(Long languageId) {
        translationRepository.deleteAllByLanguageId(languageId);
    }

    public void deleteAllBySources(Collection<Long> ids) {
        translationRepository.deleteAllBySourceIds(ids);
    }

    public void deleteAllBySource(Long id) {
        translationRepository.deleteAllBySourceId(id);

    }
}
