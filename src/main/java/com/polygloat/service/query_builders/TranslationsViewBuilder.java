package com.polygloat.service.query_builders;

import com.polygloat.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TranslationsViewBuilder {
    private final Repository repository;
    private final Set<Language> languages;
    private final String searchString;
    Set<Selection<?>> selection = new LinkedHashSet<>();
    Set<Expression<String>> fullTextFields = new HashSet<>();
    Set<Predicate> restrictions = new HashSet<>();
    private CriteriaBuilder cb;

    public TranslationsViewBuilder(CriteriaBuilder cb, Repository repository, Set<Language> languages, String searchString) {
        this.cb = cb;
        this.repository = repository;
        this.languages = languages;
        this.searchString = searchString;
    }

    public <T> CriteriaQuery<T> getBaseQuery(CriteriaQuery<T> query1) {
        Root<Source> source = query1.from(Source.class);

        Expression<String> fullPath = source.get(Source_.name);

        selection.add(source.get(Source_.id));

        selection.add(fullPath);

        Join<Source, Repository> repository = source.join(Source_.repository);

        for (Language language : languages) {
            SetJoin<Repository, Language> languages = repository.join(Repository_.languages);
            languages.on(cb.equal(languages.get(Language_.abbreviation), language.getAbbreviation()));

            SetJoin<Source, Translation> translations = source.join(Source_.translations, JoinType.LEFT);
            translations.on(cb.equal(translations.get(Translation_.language), languages));

            selection.add(languages.get(Language_.abbreviation));
            selection.add(translations.get(Translation_.text));
            fullTextFields.add(translations.get(Translation_.text));
        }

        restrictions.add(cb.equal(source.get(Source_.repository), this.repository));

        Set<Predicate> fullTextRestrictions = new HashSet<>();

        fullTextFields.add(fullPath);

        if (searchString != null && !searchString.isEmpty()) {
            for (Expression<String> fullTextField : fullTextFields) {
                fullTextRestrictions.add(cb.like(cb.upper(fullTextField), "%" + searchString.toUpperCase() + "%"));
            }
            restrictions.add(cb.or(fullTextRestrictions.toArray(new Predicate[0])));
        }

        query1.where(restrictions.toArray(new Predicate[0]));

        return query1;
    }

    @SuppressWarnings("unchecked")
    public CriteriaQuery<Object> getDataQuery() {
        CriteriaQuery<Object> query1 = getBaseQuery(cb.createQuery());

        Root<Source> source = (Root<Source>) query1.getRoots().iterator().next();

        Selection<String> fullPath = source.get(Source_.name);

        Selection<?>[] paths = selection.toArray(new Selection<?>[0]);

        query1.multiselect(paths);
        query1.orderBy(cb.asc((Expression<?>) fullPath));
        return query1;
    }

    @SuppressWarnings("unchecked")
    public CriteriaQuery<Long> getCountQuery() {
        CriteriaQuery<Long> query = getBaseQuery(cb.createQuery(Long.class));

        Root<Source> file = (Root<Source>) query.getRoots().iterator().next();

        query.select(cb.count(file));
        return query;
    }

    public static Result getData(EntityManager em, Repository repository, Set<Language> languages, String searchString, int limit, int offset) {
        TranslationsViewBuilder translationsViewBuilder = new TranslationsViewBuilder(em.getCriteriaBuilder(), repository, languages, searchString);
        Long count = em.createQuery(translationsViewBuilder.getCountQuery()).getSingleResult();
        translationsViewBuilder = new TranslationsViewBuilder(em.getCriteriaBuilder(), repository, languages, searchString);
        TypedQuery<Object> query = em.createQuery(translationsViewBuilder.getDataQuery()).setFirstResult(offset).setMaxResults(limit);
        List<Object> resultList = query.getResultList();
        return new Result(count, resultList);
    }

    @Data
    @AllArgsConstructor
    public static class Result {
        private Long count;
        private List<Object> data;
    }
}
