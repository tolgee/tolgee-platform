package io.tolgee.activity.data

/**
 * Memory-tight `Map<String, V>` implementation used for the JSONB-backed
 * fields on `ActivityModifiedEntity` (modifications, describingData,
 * describingRelations).
 *
 * A single import transaction can hold millions of these maps in
 * `ActivityHolder` until commit. A regular `LinkedHashMap` per field would
 * repeat each property-name `String` and the map's entry nodes on every
 * row — at scale this dominates heap retention and is the main reason
 * imports OOM before they reach the DB.
 *
 * Property names are interned once per entity class in a shared
 * [NameSchema]; each instance keeps only an [IntArrayList] of schema
 * indices and a raw `Object[]` of values. The exposed `Map<String, V>`
 * API and the `{"name": value, ...}` JSON shape are unchanged, so reads
 * (activity feed, listeners, deserialization) don't notice the swap.
 *
 * Concrete subclasses pick the appropriate [NameSchema.Companion.Scope]
 * and provide a Jackson deserializer that round-trips the legacy JSON shape.
 */
abstract class CompactSharedMap<V> protected constructor(
  internal val schema: NameSchema,
) : Map<String, V> {
  internal val indices = IntArrayList(2)
  internal var valuesArray: Array<Any?> = EMPTY_VALUES

  override val size: Int
    get() = indices.size

  override fun isEmpty(): Boolean = indices.isEmpty()

  fun isNotEmpty(): Boolean = !isEmpty()

  override fun containsKey(key: String): Boolean {
    val schemaIdx = schema.indexOfOrMinusOne(key)
    return schemaIdx >= 0 && indices.contains(schemaIdx)
  }

  override fun containsValue(value: V): Boolean {
    for (i in 0 until size) {
      if (valuesArray[i] == value) return true
    }
    return false
  }

  @Suppress("UNCHECKED_CAST")
  override operator fun get(key: String): V? {
    val schemaIdx = schema.indexOfOrMinusOne(key)
    if (schemaIdx < 0) return null
    val localIdx = indices.indexOf(schemaIdx)
    return if (localIdx >= 0) valuesArray[localIdx] as V? else null
  }

  override val keys: Set<String>
    get() {
      val result = LinkedHashSet<String>(size)
      for (i in 0 until size) result.add(schema.nameAt(indices[i]))
      return result
    }

  @Suppress("UNCHECKED_CAST")
  override val values: Collection<V>
    get() = (0 until size).map { valuesArray[it] as V }

  @Suppress("UNCHECKED_CAST")
  override val entries: Set<Map.Entry<String, V>>
    get() {
      val result = LinkedHashSet<Map.Entry<String, V>>(size)
      for (i in 0 until size) {
        result.add(
          java.util.AbstractMap.SimpleImmutableEntry(
            schema.nameAt(indices[i]),
            valuesArray[i] as V,
          ),
        )
      }
      return result
    }

  fun put(
    key: String,
    value: V,
  ) {
    val schemaIdx = schema.intern(key)
    val localIdx = indices.indexOf(schemaIdx)
    if (localIdx >= 0) {
      valuesArray[localIdx] = value
    } else {
      indices.add(schemaIdx)
      val newLocalIdx = indices.size - 1
      ensureCapacity(newLocalIdx + 1)
      valuesArray[newLocalIdx] = value
    }
  }

  fun addAll(other: Map<String, V>) {
    other.forEach { (k, v) -> put(k, v) }
  }

  /** For tests — exposes the shared name instance the registry holds for [name], or `null`. */
  internal fun lookupSharedName(name: String): String? {
    val idx = schema.indexOfOrMinusOne(name)
    return if (idx >= 0) schema.nameAt(idx) else null
  }

  private fun ensureCapacity(slots: Int) {
    if (valuesArray.size >= slots) return
    val newCapacity = (valuesArray.size * 2).coerceAtLeast(slots).coerceAtLeast(2)
    valuesArray = valuesArray.copyOf(newCapacity)
  }

  companion object {
    private val EMPTY_VALUES = arrayOfNulls<Any?>(0)
  }
}
