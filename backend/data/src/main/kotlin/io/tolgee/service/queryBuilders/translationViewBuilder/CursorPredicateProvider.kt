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
  @Suppress("UNCHECKED_CAST", "TYPE_MISMATCH_WARNING")
  /**
   * This function body is inspired by this thread
   * https://stackoverflow.com/questions/38017054/mysql-cursor-based-pagination-with-multiple-columns
   */
  operator fun invoke(): Predicate? {
    var result: Predicate? = null
    cursor?.entries?.reversed()?.forEach { (property, value) ->
      val isUnique = property === KeyWithTranslationsView::keyId.name
      val expression =
        selection[property] as? Expression<String>
          ?: throw BadRequestException(Message.CANNOT_SORT_BY_THIS_COLUMN)

      val strongCondition: Predicate
      val condition: Predicate
      if (value.direction == Sort.Direction.ASC) {
        condition =
          if (isUnique) {
            cb.greaterThan(expression, value.value!!)
          } else {
            cb.greaterThanOrEqualToNullable(expression, value.value)
          }
        strongCondition = cb.greaterThanNullable(expression, value.value)
      } else {
        condition =
          if (isUnique) {
            cb.lessThan(expression, value.value!!)
          } else {
            cb.lessThanOrEqualToNullable(expression, value.value)
          }
        strongCondition = cb.lessThanNullable(expression, value.value)
      }
      result = result?.let {
        cb.and(condition, cb.or(strongCondition, result))
      } ?: condition
    }
    return result
  }
}
