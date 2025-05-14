package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Handlebars
import io.tolgee.component.machineTranslation.LegacyTolgeeTranslateParams
import io.tolgee.dtos.LlmParams
import io.tolgee.model.mtServiceConfig.Formality
import io.tolgee.util.nullIfEmpty
import org.springframework.stereotype.Service

@Service
class LegacyPromptService(
  val promptVariablesService: PromptVariablesService
) {

  // max sum of lengths of the examples and close items
  val MAX_EXAMPLES_LENGTH = 2000

  fun getLegacyPrompt(): String {
    return """
      You are a translator in software localization platform, that strictly follows instructions.

      You'll get a message in {{ source }} and you respond with translated message in {{ target }}.

      Each translation has a translation key, which usually reflects the structure of the app, so similar keys are usually related.

      {{#if glossary}}
      {{#each glossary}}
      Strictly translate ```{{source}}``` as ```{{target}}```.
      {{/each}}

      {{/if}}
      {{#if examples}}
      These are some results from translation memory from the same project. You may use this as a inspiraton.

      {{#each examples}}
      {{this}}
      {{/each}}

      {{/if}}
      {{#if closeItems}}
      Here is list of translations used in the same context:

      {{#each closeItems}}
      {{this}}
      {{/each}}

      {{/if}}
      Don't add any extra dots, spaces or additional marks.
      Keep original line breaks in the text.

      Keep the style of source text.

      All translations are part of software product, don't transform them into sentences.

      {{#if projectDescription}}
      Here is user defined description for the project:
      {{projectDescription}}

      {{/if}}
      {{#if languageNote}}
      Here is user defined note:
      {{languageNote}}

      {{/if}}

      If message includes ICU parameters in curly braces, don't modify the parameter names.
      {{#if pluralFormExamples}}
      Translate ICU message plural forms, these are examples of source strings with placeholder replaced with example number
      for {{ target }}:
      {{pluralFormExamples}}

      Please include exactly these forms in the response exactly in this order: {{exactForms}}. So it will look like this:
      ```
      {{exampleIcuPlural}}
      ```
      {{/if}}

      Translation can contain also different i18n placeholder formats.
      If you spot some kind, don't translate them and keep them in the original format.

      You are working with translation key "{{ keyName }}" (no need to mention it in response).
      {{#if contextDescription}}
      User provided additional description of the key:
      {{contextDescription}}
      {{/if}}
      The message will be formatted in JSON format with this structure: {"sourceString": string}.
      {{#if closeItems}}
      You will respond with JSON formatted string with this structure: {"output": string, "contextDescription": string}.
      In "contextDescription" field describe the estimated context of the translation on the page (keep it in {{source}} language).
      {{else}}
      You will respond with JSON formatted string with this structure: {"output": string}.
      {{/if}}

      {{#if cjk}}
      Add space between {{target}} characters and latin words
      {{/if}}

      Respond with {{target}} translation without any additional information.

      {{#if formal}}
      Use formal tone.
      {{else if informal}}
      Use informal tone.
      {{/if}}
    """.trimIndent()
  }

  fun getVariables(params: LegacyTolgeeTranslateParams): MutableMap<String, Any?> {
    val result = mutableMapOf<String, Any?>()

    result.set("keyName", Handlebars.SafeString(params.keyName))
    result.set("contextDescription", params.metadata?.keyDescription?.let { Handlebars.SafeString(it) })
    result.set("source", Handlebars.SafeString(params.sourceTag))
    result.set("target", Handlebars.SafeString(params.targetTag))
    result.set(
      "projectDescription",
      params.metadata?.projectDescription?.let { Handlebars.SafeString("```\n${it}\n```") })
    result.set("languageNote", params.metadata?.languageDescription?.let { Handlebars.SafeString("```\n${it}\n```") })
    result.set("formal", params.formality == Formality.FORMAL)
    result.set("informal", params.formality == Formality.INFORMAL)
    result.set("cjk", promptVariablesService.cjkVariable(params.targetTag).value)

    var examplesLength = 0

    val closeItems = params.metadata?.closeItems
      ?.filter {
        examplesLength += it.source.length + it.target.length
        examplesLength < MAX_EXAMPLES_LENGTH
      }
      ?.map {
        mutableMapOf(
          "keyName" to it.key,
          "source" to it.source,
          "target" to it.target,
        )
      }

    result.set(
      "closeItems",
      closeItems?.map {
        jacksonObjectMapper().writeValueAsString(it)?.let { Handlebars.SafeString(it) }
      }?.nullIfEmpty()
    )

    val examples = params.metadata?.examples
      ?.filter {
        examplesLength += it.source.length + it.target.length
        examplesLength < MAX_EXAMPLES_LENGTH
      }
      ?.map {
        mutableMapOf(
          "keyName" to it.key,
          "source" to it.source,
          "target" to it.target,
        )
      }

    result.set(
      "examples",
      examples?.map {
        jacksonObjectMapper().writeValueAsString(it)?.let { Handlebars.SafeString(it) }
      }?.nullIfEmpty()
    )

    val pluralFormExamples = params.pluralFormExamples?.map {
      "${it.key} (e.g. ${it.value})"
    }?.joinToString("\n")
    result.set("pluralFormExamples", pluralFormExamples?.let { Handlebars.SafeString(it) })

    val exactForms = params.pluralFormExamples?.keys?.toList()

    val exactFormsString = exactForms?.joinToString(", ")
    result.set("exactForms", exactFormsString?.let { Handlebars.SafeString(it) })

    val exampleIcuPlural = exactForms?.let { "{count, plural, ${it.joinToString(" ") { form -> "$form {...}" }}" }
    result.set("exampleIcuPlural", exampleIcuPlural?.let { Handlebars.SafeString(it) })

    return result
  }

  fun getLlmParams(params: LegacyTolgeeTranslateParams): LlmParams {
    val variables = getVariables(params)

    val handlebars = Handlebars()

    val renderedTemplate = handlebars.compileInline(getLegacyPrompt())

    val prompt = renderedTemplate.apply(variables)

    val input = jacksonObjectMapper().writeValueAsString(mapOf("sourceString" to params.text))

    System.out.println(prompt)
    System.out.println(input)

    val messages = listOf(
      LlmParams.Companion.LlmMessage(
        type = LlmParams.Companion.LlmMessageType.TEXT,
        text = prompt,
      ),
      LlmParams.Companion.LlmMessage(
        type = LlmParams.Companion.LlmMessageType.TEXT,
        text = input,
      )
    )

    return LlmParams(
      messages,
      shouldOutputJson = true
    )
  }
}
