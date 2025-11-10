package io.tolgee.ee.service.prompt

import io.tolgee.ee.component.PromptLazyMap.Companion.Variable
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptVariableType
import org.springframework.stereotype.Component

@Component
class PromptFragmentsHelper {
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
        
        {{#if target.isCJK}}
        Add space between {{target.languageName}} characters and latin words
        {{/if}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "projectDescription",
        """
        {{#if project.description}}
        Here is user defined description for the project:
        ```
        {{project.description}}
        ```
        {{/if}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.PROJECT_DESCRIPTION,
      ),
    )

    result.add(
      Variable(
        "languageNotes",
        """
        {{#if target.languageNote}}
        Here is user defined note:
        ```
        {{target.languageNote}}
        ```
        {{/if}}          
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.LANGUAGE_NOTES,
      ),
    )

    result.add(
      Variable(
        "translationMemory",
        """
        {{#if translationMemory.json}}
        These are some results from translation memory from the same project. You may use this as a inspiraton:
        
        {{translationMemory.json}}
        {{/if}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.TM_SUGGESTIONS,
      ),
    )

    result.add(
      Variable(
        "glossary",
        """
        {{#if glossary.json}}
        These glossary terms should be strictly used, adjust form to fit into the context:
        {{glossary.json}}
        
        isCaseSensitive: If true, strictly follow the term casing, otherwise adjust casing to fit the context
        {{#if glossary.hasForbiddenTerm}}
        isForbidden = Do not use it in the resulting translation.
        {{/if}}
        {{#if glossary.hasNonTranslatable}}
        isNonTranslatable = Do not translate this term to {{target.languageName}}, keep it as it is in {{source.languageName}}.
        {{/if}}
        
        {{/if}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.GLOSSARY,
      ),
    )

    result.add(
      Variable(
        "relatedKeys",
        """
        {{#if relatedKeys.json}}
        Here is list of translations used in the same context:
        
        {{relatedKeys.json}}
        {{/if}}
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
        {{#if target.pluralFormExamples}}
        Translate ICU message plural forms, these are examples of source strings with placeholder replaced with example number
        for {{target.languageName}}:
        {{target.pluralFormExamples}}
        
        Please include exactly these forms in the response exactly in this order: {{target.exactForms}}. So it will look like this:
        ```
        {{target.exampleIcuPlural}}
        ```
        Always replace number with # in the plural.
        {{/if}}
        
        Translation can contain also different i18n placeholder formats.
        If you spot some kind, don't translate them and keep them in the original format.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "keyName",
        """
        You are working with translation key "{{key.name}}" (no need to mention it in response).
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.KEY_NAME,
      ),
    )

    result.add(
      Variable(
        "keyDescription",
        """
        {{#if key.description}}
        User provided additional description of the key:
        ```
        {{key.description}}
        ```
        {{/if}}
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
        option = BasicPromptOption.KEY_DESCRIPTION,
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
        Translate "{{source.translation}}" from {{source.languageName}} to {{target.languageName}}.
        """.trimIndent(),
        type = PromptVariableType.FRAGMENT,
      ),
    )

    result.add(
      Variable(
        "translateJson",
        """
        Return translation output and briefly describe the translation context (just a few words).
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

  companion object {
    /**
     * Signifies that model should return json, some models have a property for that
     * others just need a strong wording, so it's up to the provider class to deal with this
     */
    val LLM_MARK_JSON = "[[output_valid_json]]"
  }
}
