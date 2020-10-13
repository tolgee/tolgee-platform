package com.polygloat.service;

import com.polygloat.collections.LanguageSet;
import com.polygloat.constants.Message;
import com.polygloat.dtos.request.LanguageDTO;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Language;
import com.polygloat.model.Repository;
import com.polygloat.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LanguageService {
    private final LanguageRepository languageRepository;
    private final EntityManager entityManager;

    @Setter(onMethod = @__(@Autowired))
    private TranslationService translationService;

    @Transactional
    public Language createLanguage(LanguageDTO dto, Repository repository) {
        Language language = Language.fromRequestDTO(dto);
        language.setRepository(repository);
        repository.getLanguages().add(language);
        languageRepository.save(language);
        return language;
    }

    @Transactional
    public void deleteLanguage(Long id) {
        Language language = languageRepository.findById(id).orElseThrow(NotFoundException::new);
        translationService.deleteAllByLanguage(language.getId());
        languageRepository.delete(language);
    }

    @Transactional
    public Language editLanguage(LanguageDTO dto) {
        Language language = languageRepository.findById(dto.getId()).orElseThrow(NotFoundException::new);
        language.updateByDTO(dto);
        entityManager.persist(language);
        return language;
    }

    public LanguageSet getImplicitLanguages(Repository repository) {
        return repository.getLanguages().stream().limit(2).collect(Collectors.toCollection(LanguageSet::new));
    }

    @Transactional
    public LanguageSet findAll(Long repositoryId) {
        return new LanguageSet(languageRepository.findAllByRepositoryId(repositoryId));
    }

    public Optional<Language> findById(Long id) {
        return languageRepository.findById(id);
    }

    public Optional<Language> findByAbbreviation(String abbreviation, Repository repository) {
        return languageRepository.findByAbbreviationAndRepository(abbreviation, repository);
    }

    public Optional<Language> findByAbbreviation(String abbreviation, Long repositoryId) {
        return languageRepository.findByAbbreviationAndRepositoryId(abbreviation, repositoryId);
    }

    public LanguageSet findByAbbreviations(Collection<String> abbreviations, Long repositoryId) {
        Set<Language> langs = languageRepository.findAllByAbbreviationInAndRepositoryId(abbreviations, repositoryId);
        if (!langs.stream().map(Language::getAbbreviation).collect(Collectors.toSet()).containsAll(abbreviations)) {
            throw new NotFoundException(Message.LANGUAGE_NOT_FOUND);
        }
        return new LanguageSet(langs);
    }

    @Transactional
    public Language getOrCreate(Repository repository, String languageAbbreviation) {
        return this.findByAbbreviation(languageAbbreviation, repository)
                .orElseGet(
                        () -> this.createLanguage(
                                LanguageDTO.builder()
                                        .abbreviation(languageAbbreviation)
                                        .name(languageAbbreviation).build(),
                                repository)
                );
    }

    public LanguageSet getLanguagesForTranslationsView(Set<String> languages, Repository repository) {
        if (languages == null) {
            return getImplicitLanguages(repository);
        }
        return findByAbbreviations(languages, repository.getId());
    }

    public Optional<Language> findByName(String name, Repository repository) {
        return languageRepository.findByNameAndRepository(name, repository);
    }

    public void deleteAllByRepository(Long repositoryId) {
        languageRepository.deleteAllByRepositoryId(repositoryId);
    }
}
