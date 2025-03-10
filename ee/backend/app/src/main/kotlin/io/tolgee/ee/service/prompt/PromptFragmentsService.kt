package io.tolgee.ee.service.prompt

import io.tolgee.ee.service.prompt.PromptServiceEeImpl.Companion.Variable
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptVariableType
import org.springframework.stereotype.Service

/**
 * Signifies that model should return json, some models have a property for that
 * others just need a strong wording, so it's up to the provider class to deal with this
 */
const val LLM_MARK_JSON = "[[output_valid_json]]"

@Service
class PromptFragmentsService {
  fun getAllFragments(): MutableList<Variable> {
    val result = mutableListOf<Variable>()

    result.add(
      Variable(
        "intro",
        """
        You are a translator providing services within a software localization platform that requires strict adherence to instructions.
        Each translation is associated with a translation key, which typically reflects the structure of the app—so similar keys are often related in meaning.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "styleInfo",
        """
        Don't add any extra dots, spaces or additional marks.
        Keep original line breaks in the text.
        Keep the style of source text.
        All translations are part of software product, don't transform them into sentences.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "projectDescription",
        """
        {{#with project.description}}
        Here is user defined description for the project:
        ```
        {{this}}
        ```
        {{/with}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.PROJECT_DESCRIPTION,
      ),
    )

    result.add(
      Variable(
        "languageNotes",
        """
        {{#with target.languageNote}}
        Here is user defined note:
        ```
        {{this}}
        ```
        {{/with}}          
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.LANGUAGE_NOTES,
      ),
    )

    result.add(
      Variable(
        "translationMemory",
        """
        {{#with translationMemory.json}}
        These are some results from translation memory from the same project. You may use this as a inspiraton:
        
        {{this}}
        {{/with}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.TM_SUGGESTIONS,
      ),
    )

    result.add(
      Variable(
        "relatedKeys",
        """
        {{#with relatedKeys.json}}
        Here is list of translations used in the same context:
        
        {{this}}
        {{/with}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.KEY_CONTEXT,
      ),
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
        
        Please include exactly these forms in the response exactly in this order: {{target.exactForms}}. So it will look like this:
        ```
        {{target.exampleIcuPlural}}
        ```
        Always replace number with # in the plural.
        {{/with}}
        
        Translation can contain also different i18n placeholder formats.
        If you spot some kind, don't translate them and keep them in the original format.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "keyInfo",
        """
        You are working with translation key "{{ key.name }}" (no need to mention it in response).
        {{#with key.description}}
        User provided additional description of the key:
        ```
        {{this}}
        ```
        {{/with}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.KEY_INFO,
      ),
    )

    result.add(
      Variable(
        "screenshot",
        """{{screenshots.first}}""",
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.SCREENSHOT,
      ),
    )

    result.add(
      Variable(
        "translationInfo",
        """
        Translate "{{source.translation}}" from {{source.language}} to {{target.language}}.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "otherTranslations",
        """
        {{#each other}}
        {{#if this.translation}}
        To {{this.language}} it's translated as "{{this.translation}}"
        {{/if}}
        {{/each}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.OTHER_TRANSLATIONS,
      ),
    )

    result.add(
      Variable(
        "translateJson",
        """
        Return translation output and also describe a context in a few words.
        Follow this json format:
        ```
        {
           "output": <translation>,
           "contextDescription": <description>
        }
        ```
        $LLM_MARK_JSON
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    return result
  }
}
