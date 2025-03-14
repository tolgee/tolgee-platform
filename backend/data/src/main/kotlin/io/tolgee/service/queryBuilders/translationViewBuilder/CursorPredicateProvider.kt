package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.constants.Message
import io.tolgee.dtos.response.CursorValue
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.util.greaterThanNullable
import io.tolgee.util.greaterThanOrEqualToNullable
import io.tolgee.util.lessThanNullable
import io.tolgee.util.lessThanOrEqualToNullable
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Selection
import org.springframework.data.domain.Sort

class CursorPredicateProvider(
  private val cb: CriteriaBuilder,
  private val cursor: Map<String, CursorValue>? = null,
  private var selection: LinkedHashMap<String, Selection<*>> = LinkedHashMap(),
) {
  @Suppress("UNCHECKED_CAST")
  /**
   * This function body is inspired by this thread
   * https://stackoverflow.com/questions/38017054/mysql-cursor-based-pagination-with-multiple-columns
   */
  operator fun invoke(): Predicate? {
    var result: Predicate? = null
    cursor?.entries?.reversed()?.forEach { (property, value) ->
      val isUnique = property === KeyWithTranslationsView::keyId.name
      val selected =
        selection[property]
          ?: throw BadRequestException(Message.CANNOT_SORT_BY_THIS_COLUMN)

      // We need the runtime type of the column
      val colType = selected.javaType
      // parse the raw string from cursor into correct type for comparison
      val typedValue = parseValue(colType, value.value)

      @Suppress("UNCHECKED_CAST")
      val expression = selected as Expression<Comparable<Any>>

      val strongCondition: Predicate
      val condition: Predicate

      if (value.direction == Sort.Direction.ASC) {
        condition =
          if (isUnique) {
            val nonNullVal =
              typedValue ?: throw IllegalArgumentException(
                "Unique in the cursor cannot be null. This is bug in cursor creation.",
              )
            cb.greaterThan(expression, nonNullVal)
          } else {
            cb.greaterThanOrEqualToNullable(expression, typedValue)
          }
        strongCondition = cb.greaterThanNullable(expression, typedValue)
      } else {
        condition =
          if (isUnique) {
            val nonNullVal =
              typedValue ?: throw IllegalArgumentException(
                "Unique in the cursor cannot be null. This is bug in cursor creation.",
              )
            cb.lessThan(expression, nonNullVal)
          } else {
            cb.lessThanOrEqualToNullable(expression, typedValue)
          }
        strongCondition = cb.lessThanNullable(expression, typedValue)
      }
      result = result?.let {
        cb.and(condition, cb.or(strongCondition, result))
      } ?: condition
    }
    return result
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseValue(
    javaType: Class<*>,
    raw: String?,
  ): Comparable<Any>? {
    if (raw == null) return null

    return when (javaType) {
      String::class.java -> raw as Comparable<Any>
      java.lang.Long::class.java -> raw.toLong() as Comparable<Any>
      java.sql.Timestamp::class.java -> java.sql.Timestamp(raw.toLong()) as Comparable<Any>
      else -> throw IllegalArgumentException("Cannot parse value for type $javaType")
    }
  }
}
