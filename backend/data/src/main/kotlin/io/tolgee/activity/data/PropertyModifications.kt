package io.tolgee.activity.data

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Memory-tight `Map<String, PropertyModification>` backing
 * `ActivityModifiedEntity.modifications`.
 *
 * Same reason as [CompactSharedMap]: a single import transaction can
 * accumulate millions of activity rows, and a regular map per field
 * dominates heap retention. This one isn't a [CompactSharedMap] only
 * because [PropertyModification] is a two-field record (old + new) —
 * storing the values in a flat interleaved `[old0, new0, old1, new1, …]`
 * array avoids the wrapper object per entry.
 *
 * Property names live in a per-entity-class [NameSchema] shared across
 * all instances of that class. Jackson-instantiated instances on the
 * read path use an anonymous schema since the entity class isn't known
 * at deserialization time. The persisted JSON shape is
 * `{"name":{"old":..,"new":..}}`, identical to a plain map.
 */
@JsonDeserialize(using = PropertyModificationsDeserializer::class)
class PropertyModifications private constructor(
  private val schema: NameSchema,
) : Map<String, PropertyModification> {
  private val indices = IntArrayList(2)
  private var oldNewPairs: Array<Any?> = EMPTY_VALUES

  /** Anonymous, per-instance schema. Used by the Jackson deserializer on the read path. */
  constructor() : this(NameSchema.anonymous())

  /** Schema shared across all `PropertyModifications` for the given entity class. */
  constructor(entityClass: String) : this(NameSchema.forEntityClass(entityClass))

  override val size: Int
    get() = indices.size

  override fun isEmpty(): Boolean = indices.isEmpty()

  fun isNotEmpty(): Boolean = !isEmpty()

  private fun oldAt(localIdx: Int): Any? = oldNewPairs[localIdx shl 1]

  private fun newAt(localIdx: Int): Any? = oldNewPairs[(localIdx shl 1) or 1]

  override fun containsKey(key: String): Boolean {
    val schemaIdx = schema.indexOfOrMinusOne(key)
    return schemaIdx >= 0 && indices.contains(schemaIdx)
  }

  override fun containsValue(value: PropertyModification): Boolean {
    for (i in 0 until size) {
      if (oldAt(i) == value.old && newAt(i) == value.new) return true
    }
    return false
  }

  override operator fun get(key: String): PropertyModification? {
    val schemaIdx = schema.indexOfOrMinusOne(key)
    if (schemaIdx < 0) return null
    val localIdx = indices.indexOf(schemaIdx)
    return if (localIdx >= 0) PropertyModification(oldAt(localIdx), newAt(localIdx)) else null
  }

  override val keys: Set<String>
    get() {
      val result = LinkedHashSet<String>(size)
      for (i in 0 until size) result.add(schema.nameAt(indices[i]))
      return result
    }

  override val values: Collection<PropertyModification>
    get() = (0 until size).map { PropertyModification(oldAt(it), newAt(it)) }

  override val entries: Set<Map.Entry<String, PropertyModification>>
    get() {
      val result = LinkedHashSet<Map.Entry<String, PropertyModification>>(size)
      for (i in 0 until size) {
        result.add(
          java.util.AbstractMap.SimpleImmutableEntry(
            schema.nameAt(indices[i]),
            PropertyModification(oldAt(i), newAt(i)),
          ),
        )
      }
      return result
    }

  /** Hot-path entry point: avoids boxing values into a [PropertyModification]. */
  fun put(
    name: String,
    old: Any?,
    new: Any?,
  ) {
    val schemaIdx = schema.intern(name)
    val localIdx = indices.indexOf(schemaIdx)
    if (localIdx >= 0) {
      oldNewPairs[localIdx shl 1] = old
      oldNewPairs[(localIdx shl 1) or 1] = new
    } else {
      indices.add(schemaIdx)
      val newLocalIdx = indices.size - 1
      ensureCapacity(newLocalIdx + 1)
      oldNewPairs[newLocalIdx shl 1] = old
      oldNewPairs[(newLocalIdx shl 1) or 1] = new
    }
  }

  fun put(
    name: String,
    modification: PropertyModification,
  ) = put(name, modification.old, modification.new)

  /**
   * Named `addAll` instead of `putAll` to avoid a JVM-signature clash with the
   * `java.util.Map.putAll` we'd inherit through the [Map] interface.
   */
  fun addAll(other: Map<String, PropertyModification>) {
    other.forEach { (name, mod) -> put(name, mod) }
  }

  fun addAll(other: PropertyModifications) {
    for (i in 0 until other.size) {
      put(other.schema.nameAt(other.indices[i]), other.oldAt(i), other.newAt(i))
    }
  }

  fun remove(name: String) {
    val schemaIdx = schema.indexOfOrMinusOne(name)
    if (schemaIdx < 0) return
    val localIdx = indices.indexOf(schemaIdx)
    if (localIdx < 0) return
    indices.removeAt(localIdx)
    // Shift the interleaved pairs after [localIdx] down by one pair.
    val pairBase = localIdx shl 1
    val newSizePairs = indices.size
    val newSizeSlots = newSizePairs shl 1
    if (pairBase < newSizeSlots) {
      System.arraycopy(oldNewPairs, pairBase + 2, oldNewPairs, pairBase, newSizeSlots - pairBase)
    }
    oldNewPairs[newSizeSlots] = null
    oldNewPairs[newSizeSlots + 1] = null
  }

  /** For tests — exposes the shared name instance the registry holds for [name], or `null`. */
  internal fun lookupSharedName(name: String): String? {
    val idx = schema.indexOfOrMinusOne(name)
    return if (idx >= 0) schema.nameAt(idx) else null
  }

  private fun ensureCapacity(pairs: Int) {
    val requiredSlots = pairs shl 1
    if (oldNewPairs.size >= requiredSlots) return
    val currentCapacityPairs = oldNewPairs.size shr 1
    val newCapacityPairs = (currentCapacityPairs * 2).coerceAtLeast(pairs).coerceAtLeast(2)
    oldNewPairs = oldNewPairs.copyOf(newCapacityPairs shl 1)
  }

  companion object {
    private val EMPTY_VALUES = arrayOfNulls<Any?>(0)
  }
}

/**
 * Reads the `{"name":{"old":..,"new":..}}` JSON shape into a [PropertyModifications].
 * Serialization is handled by Jackson's default Map serializer via the [Map] interface.
 */
class PropertyModificationsDeserializer : JsonDeserializer<PropertyModifications>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): PropertyModifications {
    val mapType =
      ctxt.typeFactory.constructMapType(
        LinkedHashMap::class.java,
        String::class.java,
        PropertyModification::class.java,
      )
    val map: Map<String, PropertyModification> = ctxt.readValue(p, mapType)
    val result = PropertyModifications()
    map.forEach { (name, mod) -> result.put(name, mod) }
    return result
  }
}
