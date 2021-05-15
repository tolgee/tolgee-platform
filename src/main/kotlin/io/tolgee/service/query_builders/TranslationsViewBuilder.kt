package io.tolgee.service.query_builders

import io.tolgee.model.*
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import javax.persistence.EntityManager
import javax.persistence.criteria.*

class TranslationsViewBuilder(
    private val cb: CriteriaBuilder,
    private val repository: Repository?,
    private val languages: Set<Language>,
    private val searchString: String?
) {
    var selection: MutableSet<Selection<*>> = LinkedHashSet()
    var fullTextFields: MutableSet<Expression<String>> = HashSet()
    var restrictions: MutableSet<Predicate> = HashSet()
    fun <T> getBaseQuery(query1: CriteriaQuery<T>): CriteriaQuery<T> {
        val key = query1.from(Key::class.java)
        val fullPath: Expression<String> = key.get("name")
        selection.add(key.get<Any>("id"))
        selection.add(fullPath)
        val repositoryJoin = key.join(Key_.repository)
        for (language in languages) {
            val languagesJoin = repositoryJoin.join<Repository, Language>("languages")
            languagesJoin.on(cb.equal(languagesJoin.get(Language_.abbreviation), language.abbreviation))
            val translations = key.join<Key, Translation>("translations", JoinType.LEFT)
            translations.on(cb.equal(translations.get(Translation_.language), languagesJoin))
            selection.add(languagesJoin.get(Language_.abbreviation))
            selection.add(translations.get(Translation_.text))
            fullTextFields.add(translations.get(Translation_.text))
        }
        restrictions.add(cb.equal(key.get<Any>("repository"), repository))
        val fullTextRestrictions: MutableSet<Predicate> = HashSet()
        fullTextFields.add(fullPath)
        if (searchString != null && searchString.isNotEmpty()) {
            for (fullTextField in fullTextFields) {
                fullTextRestrictions.add(cb.like(cb.upper(fullTextField), "%" + searchString.toUpperCase() + "%"))
            }
            restrictions.add(cb.or(*fullTextRestrictions.toTypedArray()))
        }
        query1.where(*restrictions.toTypedArray())
        return query1
    }

    val dataQuery: CriteriaQuery<Any>
        get() {
            val query1 = getBaseQuery(cb.createQuery())
            val key = query1.roots.iterator().next() as Root<*>
            val fullPath: Selection<String> = key.get("name")
            val paths = selection.toTypedArray()
            query1.multiselect(*paths)
            query1.orderBy(cb.asc(fullPath as Expression<*>))
            return query1
        }
    val countQuery: CriteriaQuery<Long>
        get() {
            val query = getBaseQuery(cb.createQuery(Long::class.java))
            val file = query.roots.iterator().next() as Root<*>
            query.select(cb.count(file))
            return query
        }

    data class Result(var count: Long, var data: List<Any>) {
    }

    companion object {
        @JvmStatic
        fun getData(
            em: EntityManager,
            repository: Repository?,
            languages: Set<Language>,
            searchString: String?,
            limit: Int,
            offset: Int
        ): Result {
            var translationsViewBuilder =
                TranslationsViewBuilder(em.criteriaBuilder, repository, languages, searchString)
            val count = em.createQuery(translationsViewBuilder.countQuery).singleResult
            translationsViewBuilder = TranslationsViewBuilder(em.criteriaBuilder, repository, languages, searchString)
            val query = em.createQuery(translationsViewBuilder.dataQuery).setFirstResult(offset).setMaxResults(limit)
            val resultList = query.resultList
            return Result(count, resultList)
        }
    }
}
