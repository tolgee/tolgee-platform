package io.tolgee.service.query_builders

import io.tolgee.dtos.request.GetTranslationsParamsDto
import io.tolgee.model.*
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.*

class TranslationsViewBuilder(
        private val cb: CriteriaBuilder,
        private val project: Project?,
        private val languages: Set<Language>,
        private val params: GetTranslationsParamsDto,
        private val sort: Sort,
) {
    private var selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap()
    private var fullTextFields: MutableSet<Expression<String>> = HashSet()
    private var whereConditions: MutableSet<Predicate> = HashSet()

    private fun <T> getBaseQuery(query: CriteriaQuery<T>): CriteriaQuery<T> {
        val key = query.from(Key::class.java)
        val keyIdAttribute = key.get(Key_.id)
        selection[KeyWithTranslationsView::keyId.name] = keyIdAttribute
        val keyNameAttribute = key.get(Key_.name)
        selection[KEY_NAME_FIELD] = keyNameAttribute
        val project = key.join(Key_.project)
        for (language in languages) {
            val languagesJoin = project.join(Project_.languages)
            languagesJoin.on(cb.equal(languagesJoin.get(Language_.tag), language.tag))
            val translations = key.join(Key_.translations, JoinType.LEFT)
            translations.on(cb.equal(translations.get(Translation_.language), languagesJoin))
            selection[KeyWithTranslationsView::translations.name + "." + language.tag] = languagesJoin.get(Language_.tag)
            selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::id.name] =
                    translations.get(Translation_.id)
            val translationTextField = translations.get(Translation_.text);
            selection[KeyWithTranslationsView::translations.name + "." + language.tag + "." + TranslationView::text.name] =
                    translationTextField
            fullTextFields.add(translationTextField)
        }
        whereConditions.add(cb.equal(key.get<Any>(Key_.PROJECT), this.project))
        val fullTextRestrictions: MutableSet<Predicate> = HashSet()
        fullTextFields.add(key.get(Key_.name))

        if (params.keyName != null) {
            whereConditions.add(cb.equal(keyNameAttribute, params.keyName))
        } else if (params.keyId != null) {
            whereConditions.add(cb.equal(keyIdAttribute, params.keyId))
        } else {
            if (!params.search.isNullOrEmpty()) {
                for (fullTextField in fullTextFields) {
                    fullTextRestrictions.add(
                            cb.like(
                                    cb.upper(fullTextField),
                                    "%" + params.search.uppercase(Locale.getDefault()) + "%")
                    )
                }
                whereConditions.add(cb.or(*fullTextRestrictions.toTypedArray()))
            }
        }

        query.where(*whereConditions.toTypedArray())
        return query
    }

    @Suppress("UNCHECKED_CAST")
    private val dataQuery: CriteriaQuery<Array<Any>>
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
                    it.add(cb.asc(selection[KEY_NAME_FIELD] as Expression<*>))
                }
            }

            query1.orderBy(orderList)
            return query1
        }
    private val countQuery: CriteriaQuery<Long>
        get() {
            val query = getBaseQuery(cb.createQuery(Long::class.java))
            val file = query.roots.iterator().next() as Root<*>
            query.select(cb.count(file))
            return query
        }

    companion object {
        val KEY_NAME_FIELD = KeyWithTranslationsView::keyName.name

        @JvmStatic
        fun getData(
                em: EntityManager,
                project: Project?,
                languages: Set<Language>,
                pageable: Pageable,
                params: GetTranslationsParamsDto = GetTranslationsParamsDto()
        ): Page<KeyWithTranslationsView> {
            var translationsViewBuilder = TranslationsViewBuilder(
                    cb = em.criteriaBuilder,
                    project = project,
                    languages = languages,
                    params = params,
                    sort = pageable.sort
            )
            val count = em.createQuery(translationsViewBuilder.countQuery).singleResult
            translationsViewBuilder = TranslationsViewBuilder(em.criteriaBuilder, project, languages, params, pageable.sort)
            val query = em.createQuery(translationsViewBuilder.dataQuery).setFirstResult(pageable.offset.toInt()).setMaxResults(pageable.pageSize)
            val views = query.resultList.map { KeyWithTranslationsView(it) }
            return PageImpl(views, pageable, count)
        }
    }
}
