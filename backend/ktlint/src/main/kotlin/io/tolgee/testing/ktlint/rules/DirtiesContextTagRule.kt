package io.tolgee.testing.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * A ktlint rule that ensures that any test annotated with @DirtiesContext also has the @ContextRecreatingTest annotation.
 * This rule checks both class-level and method-level annotations.
 *
 * @ContextRecreatingTest annotation was added to mark tests to run separately in the ci/cd pipelines,
 * because they don't share the application context with other tests. @DirtiesContext has the same purpose,
 * but can't be tagged easily (gradle junit plugin would require to load a whole class to find that test is annotated).
 * Alternatively a new wrapper annotation could be added which leads to even more annotations with the same purpose.
 * So lint is actually a good option to just control that.
 *
 * However, this rule still leaves a possibility to have @ContextRecreatingTest annotation without @DirtiesContext.
 * You can use it if you want to run the test in the ci/cd pipeline in a fresh context, even though
 * the test itself doesn't dirty the context.
 */
class DirtiesContextTagRule :
  Rule(RULE_ID, About(maintainer = "Tolgee Team")),
  RuleAutocorrectApproveHandler {
  override fun beforeVisitChildNodes(
    node: ASTNode,
    emit: (Int, String, Boolean) -> AutocorrectDecision,
  ) {
    when (val psi = node.psi) {
      is KtNamedFunction -> {
        val functionAnnotations = psi.annotationEntries

        val hasDirtiesContext =
          functionAnnotations.any {
            it.shortName?.asString() == "DirtiesContext"
          }

        if (hasDirtiesContext) {
          val hasClassContextRecreatingTag =
            psi.containingClass()?.annotationEntries?.any {
              it.shortName?.asString() == "ContextRecreatingTest"
            } ?: false

          if (!hasClassContextRecreatingTag) {
            emit(node.startOffset, ERROR_MESSAGE, false)
          }
        }
      }
      is KtClass -> {
        val annotations = psi.annotationEntries

        val hasDirtiesContext =
          annotations.any {
            it.shortName?.asString() == "DirtiesContext"
          }

        if (hasDirtiesContext) {
          val hasContextRecreatingTag =
            annotations.any {
              it.shortName?.asString() == "ContextRecreatingTest"
            }

          if (!hasContextRecreatingTag) {
            emit(node.startOffset, ERROR_MESSAGE, false)
          }
        }
      }
    }
  }

  companion object {
    val RULE_ID = RuleId(RULE_ID_STR)
    const val ERROR_MESSAGE = "Tests annotated with @DirtiesContext must also include @ContextRecreatingTest"
    const val RULE_ID_STR = "tolgee:dirties-context-test-without-tag"
  }
}
