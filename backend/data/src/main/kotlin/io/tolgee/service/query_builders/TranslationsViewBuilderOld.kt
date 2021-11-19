package io.tolgee.service.query_builders

import io.tolgee.model.Language
import io.tolgee.model.Language_
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Expression
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.persistence.criteria.Selection

class TranslationsViewBuilderOld(
  private val cb: CriteriaBuilder,
  private val project: Project?,
  private val languages: Set<Language>,
  private val searchString: String?
) {
  var selection: MutableSet<Selection<*>> = LinkedHashSet()
  var fullTextFields: MutableSet<Expression<String>> = HashSet()
  var restrictions: MutableSet<Predicate> = HashSet()
  private fun <T> getBaseQuery(query1: CriteriaQuery<T>): CriteriaQuery<T> {
    val key = query1.from(Key::class.java)
    val fullPath: Expression<String> = key.get("name")
    selection.add(key.get<Any>("id"))
    selection.add(fullPath)
    val project = key.join(Key_.project)
    for (language in languages) {
      val languagesJoin = project.join<Project, Language>("languages")
      languagesJoin.on(cb.equal(languagesJoin.get(Language_.tag), language.tag))
      val translations = key.join<Key, Translation>("translations", JoinType.LEFT)
      translations.on(cb.equal(translations.get(Translation_.language), languagesJoin))
      selection.add(languagesJoin.get(Language_.tag))
      selection.add(translations.get(Translation_.text))
      fullTextFields.add(translations.get(Translation_.text))
    }
    restrictions.add(cb.equal(key.get<Any>(Key_.PROJECT), this.project))
    val fullTextRestrictions: MutableSet<Predicate> = HashSet()
    fullTextFields.add(fullPath)
    if (searchString != null && searchString.isNotEmpty()) {
      for (fullTextField in fullTextFields) {
        fullTextRestrictions.add(
          cb.like(cb.upper(fullTextField), "%" + searchString.uppercase(Locale.getDefault()) + "%")
        )
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

  data class Result(var count: Long, var data: List<Any>)

  companion object {
    @JvmStatic
    fun getData(
      em: EntityManager,
      project: Project?,
      languages: Set<Language>,
      searchString: String?,
      limit: Int,
      offset: Int
    ): Result {
      var translationsViewBuilder =
        TranslationsViewBuilderOld(em.criteriaBuilder, project, languages, searchString)
      val count = em.createQuery(translationsViewBuilder.countQuery).singleResult
      translationsViewBuilder = TranslationsViewBuilderOld(em.criteriaBuilder, project, languages, searchString)
      val query = em.createQuery(translationsViewBuilder.dataQuery).setFirstResult(offset).setMaxResults(limit)
      val resultList = query.resultList
      return Result(count, resultList)
    }
  }
}
