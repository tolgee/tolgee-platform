package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.enums.Scope
import org.springframework.stereotype.Service

/**
 * Diffs an `Array<Scope>` field (e.g. [io.tolgee.model.apps.AppInstall.grantedScopes]).
 * [DefaultPropChangesProvider] compares with `!=`, which is reference equality for arrays and so
 * reports every flush as a change while never producing a meaningful old→new value; this compares
 * the scope value sets and records the two sorted lists.
 */
@Service
class ScopeArrayPropChangesProvider : PropChangesProvider {
  override fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? {
    val oldValues = toScopeValues(old)
    val newValues = toScopeValues(new)
    if (oldValues == newValues) {
      return null
    }
    return PropertyModification(oldValues, newValues)
  }

  private fun toScopeValues(value: Any?): List<String>? {
    val elements =
      when (value) {
        null -> return null
        is Array<*> -> value.asList()
        is Collection<*> -> value.toList()
        else -> return null
      }
    return elements.mapNotNull { (it as? Scope)?.value }.sorted()
  }
}
