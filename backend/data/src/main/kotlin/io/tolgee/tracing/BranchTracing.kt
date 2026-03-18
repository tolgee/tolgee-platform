package io.tolgee.tracing

import io.opentelemetry.api.trace.Span
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import org.springframework.stereotype.Component

/**
 * Centralized tracing for branch operations.
 *
 * Provides one high-level function per operation type so that services
 * need only a single call to set all relevant span attributes.
 */
@Component
class BranchTracing(
  private val tracingContext: TolgeeTracingContext,
) {
  fun traceCreateBranch(
    projectId: Long,
    name: String,
    originBranch: Branch,
  ) {
    val span = Span.current()
    setProjectContext(span, projectId)
    span.setAttribute("tolgee.branch.name", name)
    span.setAttribute("tolgee.branch.origin.id", originBranch.id)
    span.setAttribute("tolgee.branch.origin.name", originBranch.name)
  }

  fun traceCreateBranchResult(branch: Branch) {
    Span.current().setAttribute("tolgee.branch.id", branch.id)
  }

  fun traceCopy(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val span = Span.current()
    setProjectContext(span, projectId)
    setSourceAndTarget(span, sourceBranch, targetBranch)
  }

  fun traceDryRunMerge(
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val span = Span.current()
    setProjectContext(span, sourceBranch.project.id)
    setSourceAndTarget(span, sourceBranch, targetBranch)
  }

  fun traceDryRunMergeResult(merge: BranchMerge) {
    val span = Span.current()
    span.setAttribute("tolgee.branch.merge.id", merge.id)
    span.setAttribute("tolgee.branch.merge.changesCount", merge.changes.size.toLong())
  }

  fun traceApplyMerge(merge: BranchMerge) {
    val span = Span.current()
    setProjectContext(span, merge.sourceBranch.project.id)
    span.setAttribute("tolgee.branch.merge.id", merge.id)
    setSourceAndTarget(span, merge.sourceBranch, merge.targetBranch)
    span.setAttribute("tolgee.branch.merge.changesCount", merge.changes.size.toLong())
  }

  fun traceApplyMerge(
    projectId: Long,
    mergeId: Long,
    deleteBranch: Boolean?,
    merge: BranchMerge,
  ) {
    val span = Span.current()
    setProjectContext(span, projectId)
    span.setAttribute("tolgee.branch.merge.id", mergeId)
    span.setAttribute("tolgee.branch.merge.deleteBranch", deleteBranch ?: true)
    setSourceAndTarget(span, merge.sourceBranch, merge.targetBranch)
  }

  private fun setProjectContext(
    span: Span,
    projectId: Long,
  ) {
    span.setAttribute("tolgee.project.id", projectId)
    tracingContext.setContext(projectId, null)
  }

  private fun setSourceAndTarget(
    span: Span,
    source: Branch,
    target: Branch,
  ) {
    span.setAttribute("tolgee.branch.source.id", source.id)
    span.setAttribute("tolgee.branch.source.name", source.name)
    span.setAttribute("tolgee.branch.target.id", target.id)
    span.setAttribute("tolgee.branch.target.name", target.name)
  }
}
