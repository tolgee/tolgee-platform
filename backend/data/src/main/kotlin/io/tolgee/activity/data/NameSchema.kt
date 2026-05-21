package io.tolgee.activity.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Indexed registry of property names shared across all compact-map
 * instances of the same entity class.
 *
 * The compact-map types ([PropertyModifications], [DescribingDataMap],
 * [DescribingRelationsMap]) need to hold property names without paying
 * for one `String` reference per row at activity-recording scale.
 * Instead each instance stores an int index into one of these registries
 * and looks the name up here.
 *
 * Process-wide instances live in the scope-keyed [registries]; instances
 * created without an entity class context (read path / Jackson) use
 * [anonymous] which returns a private, non-shared schema.
 *
 * The [intern] mutation path is `synchronized` because activity recording
 * for the same entity class can happen concurrently across threads.
 */
class NameSchema {
  private val names = ArrayList<String>(8)

  fun nameAt(idx: Int): String = names[idx]

  fun indexOfOrMinusOne(name: String): Int = names.indexOf(name)

  fun intern(name: String): Int {
    val idx = names.indexOf(name)
    if (idx >= 0) return idx
    synchronized(names) {
      val again = names.indexOf(name)
      if (again >= 0) return again
      names.add(name)
      return names.size - 1
    }
  }

  companion object {
    /** Registry namespaces — same entity class can have separate schemas for different fields. */
    enum class Scope { MODIFICATIONS, DESCRIBING_DATA, DESCRIBING_RELATIONS }

    private val registries: Map<Scope, ConcurrentHashMap<String, NameSchema>> =
      Scope.values().associateWith { ConcurrentHashMap<String, NameSchema>() }

    fun forEntityClass(
      entityClass: String,
      scope: Scope = Scope.MODIFICATIONS,
    ): NameSchema = registries.getValue(scope).computeIfAbsent(entityClass) { NameSchema() }

    fun anonymous(): NameSchema = NameSchema()
  }
}
