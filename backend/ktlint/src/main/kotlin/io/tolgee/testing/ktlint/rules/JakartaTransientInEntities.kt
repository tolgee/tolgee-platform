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

package io.tolgee.testing.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.children20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class JakartaTransientInEntities :
  Rule(RULE_ID, About(maintainer = "Tolgee Team")),
  RuleAutocorrectApproveHandler {
  private var seenEntityAnnotation = false
  private var exploringClass: ASTNode? = null

  override fun beforeVisitChildNodes(
    node: ASTNode,
    emit: (Int, String, Boolean) -> AutocorrectDecision,
  ) {
    when (node.elementType) {
      ElementType.IMPORT_DIRECTIVE -> {
        val qual = node.children20.last().text
        if (qual == "jakarta.persistence.Transient" || qual == "jakarta.persistence.*") {
          // Imported, nothing to do
          return stopTraversalOfAST()
        }
      }

      ElementType.CLASS -> {
        exploringClass = node
      }

      ElementType.ANNOTATION_ENTRY -> {
        node
          .children()
          .find { it.elementType == ElementType.CONSTRUCTOR_CALLEE }
          ?.children()
          ?.toList()
          ?.find { it.elementType == ElementType.TYPE_REFERENCE }
          ?.let {
            if (it.text == "Entity") {
              seenEntityAnnotation = true
            }

            if (exploringClass != null && seenEntityAnnotation && it.text == "Transient") {
              // Could propose an autocorrection by adding the import... NTH but not critical :-)
              emit(node.startOffset, ERROR_MESSAGE, false)
            }
          }
      }
    }
  }

  override fun afterVisitChildNodes(
    node: ASTNode,
    emit: (Int, String, Boolean) -> AutocorrectDecision,
  ) {
    if (node == exploringClass) {
      exploringClass = null
      seenEntityAnnotation = false
    }
  }

  companion object {
    val RULE_ID = RuleId(RULE_ID_STR)
    const val ERROR_MESSAGE = "Unexpected Kotlin-native @Transient. Import `jakarta.persistence.Transient`."
    const val RULE_ID_STR = "tolgee:jakarta-transient-in-entities"
  }
}
