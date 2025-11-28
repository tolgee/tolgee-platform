package io.tolgee.component.email.customTemplate

import io.tolgee.component.email.customTemplate.placeholder.EmailPlaceholdersExtractor
import org.springframework.stereotype.Component
import java.text.MessageFormat
import java.util.Locale
import kotlin.reflect.KClass

@Component
class EmailTemplateRenderer(
  private val placeholderExtractor: EmailPlaceholdersExtractor,
) {
  fun render(
    template: String,
    variables: EmailTemplateVariables,
  ): String {
    @Suppress("UNCHECKED_CAST")
    val entries =
      placeholderExtractor.getEntries(
        variables::class as KClass<EmailTemplateVariables>,
      )

    val parameters =
      entries
        .map { entry ->
          entry.accessor(variables) ?: ""
        }.toTypedArray()

    return MessageFormat(template, Locale.ENGLISH).format(parameters)
  }
}
