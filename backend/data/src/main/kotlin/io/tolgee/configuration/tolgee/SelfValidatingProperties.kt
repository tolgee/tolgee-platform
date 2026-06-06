package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.lang.reflect.Field
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Properties classes nested under a `@ConfigurationProperties` root are not Spring beans, so
 * `@PostConstruct` on them never runs. Implement this and let the root call [validateNestedProperties]
 * from its own `@PostConstruct` instead.
 */
interface SelfValidatingProperties {
  fun validate()
}

/**
 * Traverses only `@NestedConfigurationProperty` fields (including through collections and maps), so a
 * nested validator is reached only when every field on the path to it carries that annotation.
 */
fun validateNestedProperties(root: Any) {
  validateRecursively(root, Collections.newSetFromMap(IdentityHashMap()))
}

private fun validateRecursively(
  node: Any,
  visited: MutableSet<Any>,
) {
  if (!visited.add(node)) return
  if (node is SelfValidatingProperties) node.validate()
  nestedFields(node.javaClass).forEach { field ->
    field.isAccessible = true
    visitValue(field.get(node), visited)
  }
}

private fun visitValue(
  value: Any?,
  visited: MutableSet<Any>,
) {
  when (value) {
    null -> {}
    is Map<*, *> -> value.values.forEach { visitValue(it, visited) }
    is Iterable<*> -> value.forEach { visitValue(it, visited) }
    else -> validateRecursively(value, visited)
  }
}

private fun nestedFields(type: Class<*>): Sequence<Field> =
  generateSequence(type) { it.superclass }
    .takeWhile { it != Any::class.java }
    .flatMap { it.declaredFields.asSequence() }
    .filter { it.isAnnotationPresent(NestedConfigurationProperty::class.java) }
