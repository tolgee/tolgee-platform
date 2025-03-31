package io.tolgee.service

import io.tolgee.service.PromptService.Companion.Variable
import org.aspectj.weaver.ast.Var
import org.springframework.stereotype.Service

@Service
class PromptFragmentsService {
  fun getAllFragments(): MutableList<Variable> {

    val result = mutableListOf<Variable>()

    result.add(
      Variable(
        "translatorIntro",
        """
          You are a translator in software localization platform, that strictly follows instructions.
          Each translation has a translation key, which usually reflects the structure of the app, so similar keys are usually related.
         """.trimIndent()
      )
    )

    result.add(
      Variable(
        "translationMemory",
        """
          {{#with translationMemory.json}}
          These are some results from translation memory from the same project. You may use this as a inspiraton:
          
          {{this}}
          {{/with}}
        """.trimIndent()
      )
    )


    result.add(
      Variable(
        "relatedKeys",
        """
          {{#with relatedKeys.json}}
          Here is list of translations used in the same context:
          
          {{this}}
          {{/with}}
        """.trimIndent()
      )
    )


    result.add(
      Variable(
        "styleInfo",
        """
          Don't add any extra dots, spaces or additional marks.
          Keep original line breaks in the text.
          Keep the style of source text.
          All translations are part of software product, don't transform them into sentences.
        """.trimIndent()
      )
    )

    result.add(
      Variable(
        "icuInfo",
        """
          If message includes ICU parameters in curly braces, don't modify the parameter names.
          {{#with target.pluralFormExamples}}
          Translate ICU message plural forms, these are examples of source strings with placeholder replaced with example number
          for {{ target.language }}:
          {{this}}
          
          Please include exactly these forms in the response exactly in this order: {{exactForms}}. So it will look like this:
          ```
          {{target.exampleIcuPlural}}
          ```
          Always replace number with # in the plural.
          {{/with}}
          
          Translation can contain also different i18n placeholder formats.
          If you spot some kind, don't translate them and keep them in the original format.
        """.trimIndent()
      )
    )

    result.add(
      Variable(
        "keyInfo",
        """
          You are working with translation key "{{ key.name }}" (no need to mention it in response).
          {{#with key.description}}
          User provided additional description of the key:
          {{this}}
          {{/with}}
        """.trimIndent()
      )
    )

    result.add(
      Variable(
        "translationInfo",
        """
          Translate ```{{source.translation}}``` from {{source.language}} to {{target.language}}.
        """.trimIndent()
      )
    )


    result.add(
      Variable(
        "translateJson",
        """
          Return result in json
          ```
          {
             "output": <translation>,
             "contextDescription": <description>
          }
          ```
        """.trimIndent(),
      ),
    )

    return result
  }
}
