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

package io.tolgee.testing.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import io.tolgee.testing.ktlint.rules.DirtiesContextTagRule
import io.tolgee.testing.ktlint.rules.JakartaTransientInEntities

class TolgeeRulesProvider : RuleSetProviderV3(RULE_SET_ID) {
  override fun getRuleProviders(): Set<RuleProvider> =
    setOf(
      RuleProvider {
        JakartaTransientInEntities()
      },
      RuleProvider {
        DirtiesContextTagRule()
      },
    )

  companion object {
    val RULE_SET_ID = RuleSetId(RULE_SET_ID_STR)
    const val RULE_SET_ID_STR = "tolgee"
  }
}
