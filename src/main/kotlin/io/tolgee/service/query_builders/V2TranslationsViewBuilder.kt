package io.tolgee.service.query_builders

import io.tolgee.model.*
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.views.KeyTranslationsView
import io.tolgee.model.views.TranslationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.*

class V2TranslationsViewBuilder(
        private val cb: CriteriaBuilder,
        private val project: Project?,
        private val languages: Set<Language>,
        private val searchString: String?,
        private val sort: Sort,
) {
    var selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap()
    var fullTextFields: MutableSet<Expression<String>> = HashSet()
    var whereConditions: MutableSet<Predicate> = HashSet()
    private fun <T> getBaseQuery(query: CriteriaQuery<T>): CriteriaQuery<T> {
        val key = query.from(Key::class.java)
        selection[KEY_ID_FIELD_NAME] = key.get(Key_.id)
        selection[KeyTranslationsView::keyName.name] = key.get(Key_.name)
        val project = key.join(Key_.project)
        for (language in languages) {
            val languagesJoin = project.join(Project_.languages)
            languagesJoin.on(cb.equal(languagesJoin.get(Language_.tag), language.tag))
            val translations = key.join(Key_.translations, JoinType.LEFT)
            translations.on(cb.equal(translations.get(Translation_.language), languagesJoin))
            selection[KeyTranslationsView::translations.name + "." + language.tag] = languagesJoin.get(Language_.tag)
            selection[KeyTranslationsView::keyName.name + "." + language.tag + "." + TranslationView::id.name] =
                    translations.get(Translation_.id)
            selection[KeyTranslationsView::keyName.name + "." + language.tag + "." + TranslationView::text.name] =
                    translations.get(Translation_.text)
        }
        whereConditions.add(cb.equal(key.get<Any>(Key_.PROJECT), this.project))
        val fullTextRestrictions: MutableSet<Predicate> = HashSet()
        fullTextFields.add(key.get(Key_.name))
        if (!searchString.isNullOrEmpty()) {
            for (fullTextField in fullTextFields) {
                fullTextRestrictions.add(cb.like(cb.upper(fullTextField), "%" + searchString.uppercase(Locale.getDefault()) + "%"))
            }
            whereConditions.add(cb.or(*fullTextRestrictions.toTypedArray()))
        }
        query.where(*whereConditions.toTypedArray())
        return query
    }

    @Suppress("UNCHECKED_CAST")
    val dataQuery: CriteriaQuery<Array<Any>>
        get() {
            val query1 = getBaseQuery(cb.createQuery(Array<Any>::class.java))
            val paths = selection.values.toTypedArray()
            query1.multiselect(*paths)

            val orderList = sort.asSequence().filter { selection[it.property] != null }.map {
                val expression = selection[it.property] as Expression<*>
                when (it.direction) {
                    Sort.Direction.DESC -> cb.desc(expression)
                    else -> cb.asc(expression)
                }
            }.toMutableList().also {
                if (it.isEmpty()) {
                    it.add(cb.asc(selection[KEY_ID_FIELD_NAME] as Expression<*>))
                }
            }

            query1.orderBy(orderList)
            return query1
        }
    val countQuery: CriteriaQuery<Long>
        get() {
            val query = getBaseQuery(cb.createQuery(Long::class.java))
            val file = query.roots.iterator().next() as Root<*>
            query.select(cb.count(file))
            return query
        }

    companion object {
        val KEY_ID_FIELD_NAME = KeyTranslationsView::keyId.name

        @JvmStatic
        fun getData(
                em: EntityManager,
                project: Project?,
                languages: Set<Language>,
                searchString: String?,
                pageable: Pageable
        ): Page<KeyTranslationsView> {
            var translationsViewBuilder = V2TranslationsViewBuilder(
                    cb = em.criteriaBuilder,
                    project = project,
                    languages = languages,
                    searchString = searchString,
                    sort = pageable.sort
            )
            val count = em.createQuery(translationsViewBuilder.countQuery).singleResult
            translationsViewBuilder = V2TranslationsViewBuilder(em.criteriaBuilder, project, languages, searchString, pageable.sort)
            val query = em.createQuery(translationsViewBuilder.dataQuery).setFirstResult(pageable.offset.toInt()).setMaxResults(pageable.pageSize)
            val views = query.resultList.map { KeyTranslationsView(it) }
            return PageImpl(views, pageable, count)
        }
    }
}
