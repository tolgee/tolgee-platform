/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.email

import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.ITemplateContext
import org.thymeleaf.messageresolver.IMessageResolver
import java.util.regex.Pattern

/**
 * Transforms a classic message resolver into an XML-aware message resolver.
 * The transformation performed is specific to emails template structure, and isn't suitable for general-purpose use.
 */
class EmailMessageResolver(
  private val provider: IMessageResolver,
  private val templateEngine: TemplateEngine,
) : IMessageResolver by provider {
  override fun resolveMessage(
    context: ITemplateContext?,
    origin: Class<*>?,
    key: String,
    messageParameters: Array<out Any?>?,
  ): String? {
    val message = provider.resolveMessage(context, origin, key, messageParameters)
    if (message == null || context == null) return null

    return postProcessMessage(message, context, key)
  }

  private fun postProcessMessage(
    message: String,
    context: ITemplateContext,
    code: String,
  ): String {
    // Dumb heuristic to skip XML parsing for trivial cases, to (hopefully) improve performance.
    if (message.contains('<') && message.contains('>')) {
      var counter = 0L

      val stack = mutableListOf(code.replace(".", "--"))
      val m = XML_PATTERN.matcher(message)

      // Does not contain lightweight XML references. Skip.
      if (!m.find()) return message.replace("\n", "<br/>")

      val template =
        m.replaceAll {
          if (it.group(1) == null) {
            // Opening tag
            stack.add(it.group(2))

            val ref = "intl-ref-${++counter}"
            val fragName = "intl-${stack.joinToString("--")}"
            val fragExpr = "~{ :: $fragName (~{ :: $ref }) }"

            "<th:block th:replace=\"$fragExpr\"><th:block th:ref=\"$ref\">"
          } else {
            // Closing tag
            stack.removeLast()

            "</th:block></th:block>"
          }
        }

      return templateEngine.process("<!--@frag--> $template", context).replace("\n", "<br/>")
    }

    return message.replace("\n", "<br/>")
  }

  companion object {
    // KISS. Matches attribute-less, non-self-closing tags.
    // Reference: https://formatjs.github.io/docs/core-concepts/icu-syntax#rich-text-formatting
    private val XML_PATTERN = Pattern.compile("<(/)?([a-zA-Z-]+)>")
  }
}
