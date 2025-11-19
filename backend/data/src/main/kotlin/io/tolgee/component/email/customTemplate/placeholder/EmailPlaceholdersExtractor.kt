package io.tolgee.component.email.customTemplate.placeholder

import io.tolgee.component.email.customTemplate.EmailTemplateVariables
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Component
class EmailPlaceholdersExtractor {

  private val cache =
    ConcurrentHashMap<KClass<*>, List<EmailPlaceholderEntry<*>>>()

  fun <T : EmailTemplateVariables> getEntries(kClass: KClass<T>): List<EmailPlaceholderEntry<T>> {
    val existing = cache[kClass]
    if (existing != null) {
      @Suppress("UNCHECKED_CAST")
      return existing as List<EmailPlaceholderEntry<T>>
    }

    val extracted = extract(kClass)
    cache[kClass] = extracted
    return extracted
  }

  fun <T : EmailTemplateVariables> getDefinitions(kClass: KClass<T>): List<EmailPlaceholderDefinition> {
    return getEntries(kClass).map { it.definition }
  }

  private fun <T : EmailTemplateVariables> extract(kClass: KClass<T>): List<EmailPlaceholderEntry<T>> {
    return kClass.memberProperties.mapNotNull { property ->
      val annotation = property.findAnnotation<EmailPlaceholder>() ?: return@mapNotNull null

      EmailPlaceholderEntry(
        definition = EmailPlaceholderDefinition(
          position = annotation.position,
          placeholder = annotation.placeholder,
          description = annotation.description,
          exampleValue = annotation.exampleValue,
        ),
        accessor = { instance: T ->
          property.get(instance)?.toString()
        }
      )
    }.sortedBy { it.definition.position }
  }
}
