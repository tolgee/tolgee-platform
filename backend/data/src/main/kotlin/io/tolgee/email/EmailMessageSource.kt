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

import com.transferwise.icu.ICUMessageSource
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException
import java.util.Locale
import java.util.regex.Pattern

/**
 * Transforms a classic ICUMessageSource into an XML-aware message source.
 * The transformation performed is specific to emails template structure, and isn't suitable for general-purpose use.
 *
 * Cannot be written as a subclass of ICUReloadableResourceBundleMessageSource as its `getMessage` methods are final.
 */
class EmailMessageSource(
	private val provider: ICUMessageSource,
	private val emailTemplateUtils: EmailTemplateUtils
) : ICUMessageSource {
	private var counter = 0L

	override fun getMessage(
		code: String,
		args: Array<out Any?>?,
		defaultMessage: String?,
		locale: Locale
	): String? {
		return provider.getMessage(code, args, defaultMessage, locale)?.postProcessMessage(code)
	}

	override fun getMessage(
		code: String,
		args: Array<out Any?>?,
		locale: Locale
	): String {
		return provider.getMessage(code, args, locale).postProcessMessage(code)
	}

	override fun getMessage(
		code: String,
		args: Map<String, Any?>?,
		defaultMessage: String?,
		locale: Locale
	): String? {
		return provider.getMessage(code, args, defaultMessage, locale)?.postProcessMessage(code)
	}

	override fun getMessage(
		code: String,
		args: Map<String, Any?>?,
		locale: Locale
	): String? {
		return provider.getMessage(code, args, locale).postProcessMessage(code)
	}

	override fun getMessage(
		resolvable: MessageSourceResolvable,
		locale: Locale
	): String {
		resolvable.codes?.forEach { code ->
			getMessage(code, resolvable.arguments, null, locale)?.let {
				return it
			}
		}

		throw NoSuchMessageException(resolvable.codes?.lastOrNull() ?: "", locale)
	}

	override fun getParentMessageSource(): MessageSource? {
		return provider.parentMessageSource
	}

	override fun setParentMessageSource(parent: MessageSource?) {
		provider.parentMessageSource = parent
	}

	@Synchronized
	private fun count(): Long {
		val ret = counter
		if (counter == Long.MAX_VALUE) {
			counter = 0L
		} else {
			counter += 1L
		}
		return ret
	}

	private fun makeRef(): String = "intl-ref-${count()}"

	private fun String.postProcessMessage(code: String): String {
		var str = emailTemplateUtils.escape(this).replace("\n", "<br/>")

		// Dumb heuristic to skip XML parsing for trivial cases, to improve performance.
		if (contains('<') && contains('>')) {
			var delta = 0
			val stack = mutableListOf(code)
			val m = XML_PATTERN.matcher(str)

			while (m.find()) {
				if (m.group(1) == null) {
					stack.add(m.group(2))

					val ref = makeRef()
					val replacement = "<div th:ref=\"$ref\" th:replace=\"~{::intl-${stack.joinToString(
            "--"
          )}(~{:: $ref/content})}\"><content th:remove=\"tag\">"
					str = str.replaceRange(delta + m.start(), delta + m.end(), replacement)
					delta += replacement.length - (m.end() - m.start())
				} else {
					stack.removeLast()

					val replacement = "</content></div>"
					str = str.replaceRange(delta + m.start(), delta + m.end(), replacement)
					delta += replacement.length - (m.end() - m.start())
				}
			}
		}

		return str
	}

	companion object {
		private val XML_PATTERN = Pattern.compile("<(/)?([a-zA-Z-]+)>")
	}
}
