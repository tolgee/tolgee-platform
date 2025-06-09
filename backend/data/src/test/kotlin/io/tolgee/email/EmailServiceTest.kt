/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
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

import io.tolgee.configuration.tolgee.SmtpProperties
import io.tolgee.testing.assert
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.assertj.core.api.AbstractStringAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@Component
@ExtendWith(SpringExtension::class)
@Import(EmailService::class, EmailTemplateConfig::class)
class EmailServiceTest {
	@MockBean
	private lateinit var smtpProperties: SmtpProperties

	@MockBean
	private lateinit var mailSender: JavaMailSender

	@Autowired
	private lateinit var emailService: EmailService

	@Captor
	private lateinit var emailCaptor: ArgumentCaptor<MimeMessage>

	@BeforeEach
	fun beforeEach() {
		val sender = JavaMailSenderImpl()
		whenever(smtpProperties.from).thenReturn("Tolgee Test <robomouse+test@tolgee.test>")
		whenever(mailSender.createMimeMessage()).let {
			val msg = sender.createMimeMessage()
			it.thenReturn(msg)
		}
	}

	@Test
	fun `it sends a rendered email with variables and ICU strings processed`() {
		emailService.sendEmailTemplate("test@tolgee.text", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES)
		verify(mailSender).send(emailCaptor.capture())

		val email = emailCaptor.value
		email.subject.assert.isEqualTo("Test email (written with React Email)")
		email.allRecipients.asList().assert.singleElement().asString().isEqualTo("test@tolgee.text")

		email.assertContents()
			.contains("Testing ICU strings -- test!!")
			.contains("Value of `testVar`: <span>test!!</span>")
			.contains("<span>Was `testVar` equal to &quot;meow&quot; : </span><span>no</span>")
			// Makes sure tag handling works as expected
			.contains(
				"Powered by <a href=\"https://tolgee.io\" style=\"color:inherit;text-decoration-line:underline\" " +
					"target=\"_blank\">Tolgee</a>",
			)
			// Might be a bit brittle but does the trick for now.
			.doesNotContain(" th:")
			.doesNotContain(" data-th")
	}

	@Test
	fun `it correctly processes conditional blocks`() {
		// FWIW this is very close to just testing Thymeleaf itself, but it serves as a sanity check for the template itself
		emailService.sendEmailTemplate("test@tolgee.text", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES_MEOW)
		verify(mailSender).send(emailCaptor.capture())

		val email = emailCaptor.value
		email.assertContents()
			.contains("Testing ICU strings -- meow")
			.contains("Value of `testVar`: <span>meow</span>")
			.contains("<span>Was `testVar` equal to &quot;meow&quot; : </span><span>yes</span>")
	}

	@Test
	fun `it correctly processes foreach blocks`() {
		// FWIW this is very close to just testing Thymeleaf itself, but it serves as a sanity check for the template itself
		emailService.sendEmailTemplate("test@tolgee.text", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES_MEOW)
		verify(mailSender).send(emailCaptor.capture())

		val email = emailCaptor.value
		email.assertContents()
			.contains("Plain test: <span>Name #1</span>")
			.contains("<span>ICU test: Name &#35;1</span>")
			.contains("Plain test: <span>Name #2</span>")
			.contains("<span>ICU test: Name &#35;2</span>")
			.contains("Plain test: <span>Name #3</span>")
			.contains("<span>ICU test: Name &#35;3</span>")
	}

	@Test
	fun `it is not vulnerable to injection`() {
		emailService.sendEmailTemplate("test@tolgee.text", "zz-test-email", Locale.ENGLISH, TEST_PROPERTIES_INJECT)
		verify(mailSender).send(emailCaptor.capture())

		val email = emailCaptor.value
		email.assertContents()
			.doesNotContain("<a href=\"https://pwned.example.com\">")
			.contains("&lt;a href=&quot;https://pwned.example.com&quot;")
	}

	private fun MimeMessage.assertContents(): AbstractStringAssert<*> {
		return this.content
			.let { it as MimeMultipart }
			.let { it.getBodyPart(0).content as MimeMultipart }
			.let { it.getBodyPart(0).content as String }
			.assert
	}

	companion object {
		private val TEST_PROPERTIES =
			mapOf(
				"testVar" to "test!!",
				"testList" to
					listOf(
						mapOf("name" to "Name #1"),
						mapOf("name" to "Name #2"),
						mapOf("name" to "Name #3"),
					),
			)

		private val TEST_PROPERTIES_MEOW =
			mapOf(
				"testVar" to "meow",
				"testList" to
					listOf(
						mapOf("name" to "Name #1"),
						mapOf("name" to "Name #2"),
						mapOf("name" to "Name #3"),
					),
			)

		private val TEST_PROPERTIES_INJECT =
			mapOf(
				"testVar" to "<a href=\"https://pwned.example.com\">totally legit text</a>",
				"testList" to emptyList<Void>(),
			)
	}
}
