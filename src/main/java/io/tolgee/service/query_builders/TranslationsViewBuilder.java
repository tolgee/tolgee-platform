package io.tolgee.service.query_builders;

import io.tolgee.model.*;

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
        Root<Key> key = query1.from(Key.class);

        Expression<String> fullPath = key.get("name");

        selection.add(key.get("id"));

        selection.add(fullPath);

        Join<Key, Repository> repositoryJoin = key.join("repository");

        for (Language language : languages) {
            Join<Repository, Language> languagesJoin = repositoryJoin.join("languages");
            languagesJoin.on(cb.equal(languagesJoin.get(Language_.abbreviation), language.getAbbreviation()));

            Join<Key, Translation> translations = key.join("translations", JoinType.LEFT);
            translations.on(cb.equal(translations.get(Translation_.language), languagesJoin));

            selection.add(languagesJoin.get(Language_.abbreviation));
            selection.add(translations.get(Translation_.text));
            fullTextFields.add(translations.get(Translation_.text));
        }

        restrictions.add(cb.equal(key.get("repository"), this.repository));

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

        Root<Key> key = (Root<Key>) query1.getRoots().iterator().next();

        Selection<String> fullPath = key.get("name");

        Selection<?>[] paths = selection.toArray(new Selection<?>[0]);

        query1.multiselect(paths);
        query1.orderBy(cb.asc((Expression<?>) fullPath));
        return query1;
    }

    @SuppressWarnings("unchecked")
    public CriteriaQuery<Long> getCountQuery() {
        CriteriaQuery<Long> query = getBaseQuery(cb.createQuery(Long.class));

        Root<Key> file = (Root<Key>) query.getRoots().iterator().next();

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

    public static class Result {
        private Long count;
        private List<Object> data;

        public Result(Long count, List<Object> data) {
            this.count = count;
            this.data = data;
        }

        public Long getCount() {
            return this.count;
        }

        public List<Object> getData() {
            return this.data;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public void setData(List<Object> data) {
            this.data = data;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Result)) return false;
            final Result other = (Result) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$count = this.getCount();
            final Object other$count = other.getCount();
            if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
            final Object this$data = this.getData();
            final Object other$data = other.getData();
            if (this$data == null ? other$data != null : !this$data.equals(other$data)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof Result;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $count = this.getCount();
            result = result * PRIME + ($count == null ? 43 : $count.hashCode());
            final Object $data = this.getData();
            result = result * PRIME + ($data == null ? 43 : $data.hashCode());
            return result;
        }

        public String toString() {
            return "TranslationsViewBuilder.Result(count=" + this.getCount() + ", data=" + this.getData() + ")";
        }
    }
}
