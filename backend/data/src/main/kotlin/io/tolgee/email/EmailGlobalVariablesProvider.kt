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

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URISyntaxException

@Component
class EmailGlobalVariablesProvider(
	// Used to identify whether we're Tolgee Cloud or not
	private val billingConfigProvider: PublicBillingConfProvider,
	private val tolgeeProperties: TolgeeProperties,
) {
	operator fun invoke(): Map<String, Any?> {
		val isCloud = billingConfigProvider().enabled

		return mapOf(
			"isCloud" to isCloud,
			"instanceQualifier" to if (isCloud) tolgeeProperties.appName else tolgeeProperties.frontEndUrl.intoQualifier(),
			"instanceUrl" to tolgeeProperties.frontEndUrl,
		)
	}

	private fun String?.intoQualifier(): String {
		return this?.let {
			try {
				return@let URI(it).host
			} catch (_: URISyntaxException) {
				return@let null
			}
		} ?: SELF_HOSTED_DEFAULT_QUALIFIER
	}

	companion object {
		// Not ideal because not translated... But shouldn't show up on properly configured instances :)
		const val SELF_HOSTED_DEFAULT_QUALIFIER = "a self-hosted instance"
	}
}
