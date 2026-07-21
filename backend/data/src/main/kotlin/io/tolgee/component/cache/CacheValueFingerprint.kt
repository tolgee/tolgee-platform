package io.tolgee.component.cache

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/**
 * Produces a deterministic, structural fingerprint of a cached value's type.
 *
 * The fingerprint changes whenever the serialized shape of the type could change between versions
 * (a property added/removed/retyped anywhere in the graph, an enum constant set changed). It stays
 * stable across restarts and JVMs for an unchanged type, and is unaffected by behavior-only changes.
 *
 * Only project types (package [PROJECT_PACKAGE_PREFIX]) are expanded into their properties; JDK and
 * Kotlin-stdlib types are treated as opaque leaves keyed by name plus type arguments, because their
 * serialized form is owned by the library, not by us. Recursion is bounded by a per-walk set that
 * breaks reference cycles.
 */
@Component
class CacheValueFingerprint {
  fun compute(type: KClass<*>): String = compute(type.starProjectedType)

  fun compute(type: KType): String = shortHash(signature(type))

  fun compute(types: Collection<KType>): String = shortHash(types.map { signature(it) }.sorted().joinToString("|"))

  internal fun signature(type: KType): String = StringBuilder().also { appendType(type, it, HashSet()) }.toString()

  private fun appendType(
    type: KType,
    out: StringBuilder,
    expanding: MutableSet<String>,
  ) {
    val klass = type.jvmErasure
    appendErasure(klass, out, expanding)
    if (type.arguments.isNotEmpty()) {
      out.append('<')
      type.arguments.forEachIndexed { index, projection ->
        if (index > 0) out.append(',')
        val argType = projection.type
        if (argType == null) {
          out.append('*')
          return@forEachIndexed
        }
        appendType(argType, out, expanding)
      }
      out.append('>')
    }
    if (type.isMarkedNullable) out.append('?')
  }

  private fun appendErasure(
    klass: KClass<*>,
    out: StringBuilder,
    expanding: MutableSet<String>,
  ) {
    val name = klass.qualifiedName ?: klass.java.name
    if (klass.java.isEnum) {
      out.append(name).append('{')
      klass.java.enumConstants
        .map { (it as Enum<*>).name }
        .sorted()
        .joinTo(out, ",")
      out.append('}')
      return
    }
    if (!shouldExpand(name)) {
      out.append(name)
      return
    }
    if (!expanding.add(name)) {
      out.append('@').append(name)
      return
    }
    out.append(name).append('(')
    klass.memberProperties
      .sortedBy { it.name }
      .forEachIndexed { index, property ->
        if (index > 0) out.append(',')
        out.append(property.name).append(':')
        appendType(property.returnType, out, expanding)
      }
    out.append(')')
    expanding.remove(name)
  }

  private fun shouldExpand(qualifiedName: String): Boolean = qualifiedName.startsWith(PROJECT_PACKAGE_PREFIX)

  private fun shortHash(signature: String): String = DigestUtils.sha256Hex(signature).substring(0, FINGERPRINT_LENGTH)

  companion object {
    private const val PROJECT_PACKAGE_PREFIX = "io.tolgee"
    private const val FINGERPRINT_LENGTH = 12
  }
}
