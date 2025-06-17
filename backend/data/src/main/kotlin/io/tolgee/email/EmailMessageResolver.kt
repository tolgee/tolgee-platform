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

import org.springframework.context.ApplicationContext
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
  applicationContext: ApplicationContext,
) : IMessageResolver by provider {
  private val templateEngine by lazy {
    applicationContext.getBean("emailTemplateEngine", TemplateEngine::class.java)
  }

  override fun resolveMessage(
    context: ITemplateContext?,
    origin: Class<*>?,
    key: String,
    messageParameters: Array<out Any?>?
  ): String? {
    val message = provider.resolveMessage(context, origin, key, messageParameters)
    if (message == null || context == null) return null

    return postProcessMessage(message, context, key)
  }

  // The likelihood of overflowing a 64-bit number is virtually zero
  // Don't really care if some dupes happen across threads, it'll be good enough
  private var counter = 0L
  private fun makeRef(): String = "intl-ref-${++counter}"

  private fun postProcessMessage(message: String, context: ITemplateContext, code: String): String {
    val templateId = context.templateData.templateResource.baseName
    var str = message

    // Dumb heuristic to skip XML parsing for trivial cases, to improve performance.
    if (str.contains('<') && str.contains('>')) {
      var delta = 0
      var found = false

      val stack = mutableListOf(code.replace(".", "--"))
      val m = XML_PATTERN.matcher(str)

      while (m.find()) {
        found = true
        if (m.group(1) == null) {
          stack.add(m.group(2))

          val ref = makeRef()
          val fragName = "intl-${stack.joinToString("--")}"
          val fragExpr = "~{ $templateId :: $fragName (~{ :: $ref }) }"
          val replacement =
            "<th:block th:replace=\"$fragExpr\">" +
            "<th:block th:ref=\"$ref\">"

          str = str.replaceRange(delta + m.start(), delta + m.end(), replacement)
          delta += replacement.length - (m.end() - m.start())
        } else {
          stack.removeLast()

          val replacement = "</th:block></th:block>"
          str = str.replaceRange(delta + m.start(), delta + m.end(), replacement)
          delta += replacement.length - (m.end() - m.start())
        }
      }

      if (found) {
        return templateEngine.process("<!--@frag--> $str", context).replace("\n", "<br/>")
      }
    }

    return str.replace("\n", "<br/>")
  }

  companion object {
    private val XML_PATTERN = Pattern.compile("<(/)?([a-zA-Z-]+)>")
  }
}
