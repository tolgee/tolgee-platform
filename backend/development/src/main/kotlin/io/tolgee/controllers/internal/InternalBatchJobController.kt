package io.tolgee.controllers.internal

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.NoOpMultiRequest
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.batch.timing.BatchJobOperationTimer
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.batch.BatchJob
import jakarta.persistence.EntityManager
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@InternalController(["internal/batch-jobs"])
class InternalBatchJobController(
  private val batchJobService: BatchJobService,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val operationTimer: BatchJobOperationTimer?,
  private val entityManager: EntityManager,
) {
  @PostMapping("/start-no-op")
  fun startNoOpJob(
    @Valid @RequestBody
    data: NoOpRequest,
  ): BatchJobModel {
    return batchJobService
      .startJob(
        data,
        project = null,
        author = null,
        type = BatchJobType.NO_OP,
      ).model
  }

  @PostMapping("/start-no-op-multi")
  @Transactional
  fun startNoOpMultiJob(
    @RequestBody
    data: NoOpMultiRequest,
  ): List<BatchJobModel> {
    val projects = createTestProjects(data.numberOfProjects)
    val jobType =
      if (projects.isNotEmpty()) BatchJobType.NO_OP_EXCLUSIVE else BatchJobType.NO_OP

    val itemsPerJob = data.totalItems / data.numberOfJobs
    val remainder = data.totalItems % data.numberOfJobs
    var nextId = 1L

    return (0 until data.numberOfJobs).map { jobIndex ->
      val jobItemCount = itemsPerJob + if (jobIndex < remainder) 1 else 0
      val request = NoOpRequest()
      request.itemIds = (nextId until nextId + jobItemCount).toList()
      request.chunkProcessingDelayMs = data.chunkProcessingDelayMs
      nextId += jobItemCount

      val project = if (projects.isNotEmpty()) projects[jobIndex % projects.size] else null

      batchJobService
        .startJob(
          request,
          project = project,
          author = null,
          type = jobType,
        ).model
    }
  }

  @GetMapping("/timing-report")
  fun getTimingReport(): Map<String, Map<String, Any>> {
    return operationTimer?.getReport() ?: emptyMap()
  }

  @PostMapping("/timing-reset")
  fun resetTiming() {
    operationTimer?.reset()
  }

  private fun createTestProjects(numberOfProjects: Int): List<Project> {
    if (numberOfProjects <= 0) return emptyList()

    val org = getOrCreateTestOrganization()

    return (1..numberOfProjects).map { i ->
      val project = Project()
      project.name = "perf-test-project-$i-${System.currentTimeMillis()}"
      project.organizationOwner = org
      entityManager.persist(project)
      project
    }
  }

  private fun getOrCreateTestOrganization(): Organization {
    val existing =
      entityManager
        .createQuery(
          "SELECT o FROM Organization o WHERE o.slug = :slug",
          Organization::class.java,
        ).setParameter("slug", "perf-test-org")
        .resultList
        .firstOrNull()

    if (existing != null) return existing

    val org = Organization()
    org.name = "Perf Test Org"
    org.slug = "perf-test-org"
    entityManager.persist(org)

    val permission = Permission()
    permission.organization = org
    entityManager.persist(permission)

    org.basePermission = permission
    entityManager.flush()

    return org
  }

  private val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))
}
