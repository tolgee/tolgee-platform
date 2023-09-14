package io.tolgee.repository

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.views.JobErrorMessagesView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, Long> {
  @Query(
    value = """
    select j from BatchJob j
    left join fetch j.author
    left join fetch j.activityRevision
    where j.project.id = :projectId
    and (:userAccountId is null or j.author.id = :userAccountId)
    """,
    countQuery = """
    select count(j) from BatchJob j
    where j.project.id = :projectId
    and (:userAccountId is null or j.author.id = :userAccountId)
    """
  )
  fun getJobs(projectId: Long, userAccountId: Long?, pageable: Pageable): Page<BatchJob>

  @Query(
    value = """
    select j from BatchJob j
    left join fetch j.author
    left join fetch j.activityRevision
    where j.project.id = :projectId and (j.hidden = false or j.status = 'FAILED')
    and (:userAccountId is null or j.author.id = :userAccountId)
    and (j.status not in :completedStatuses or j.updatedAt > :oneHourAgo)
    order by j.updatedAt
    """
  )
  fun getCurrentJobs(
    projectId: Long,
    userAccountId: Long?,
    oneHourAgo: Date,
    completedStatuses: List<BatchJobStatus>
  ): List<BatchJob>

  @Query(
    nativeQuery = true,
    value = """
     select tolgee_batch_job_chunk_execution.batch_job_id, sum(jsonb_array_length(success_targets)) 
      from tolgee_batch_job_chunk_execution 
      where batch_job_id in :jobIds
      group by tolgee_batch_job_chunk_execution.batch_job_id
  """
  )
  fun getProgresses(jobIds: List<Long>): List<Array<Any>>

  @Query(
    value = """
     select bjce.batchJob.id as batchJobId, bjce.id as executionId, bjce.errorMessage as errorMessage, bjce.updatedAt as updatedAt 
     from BatchJobChunkExecution bjce 
       where bjce.batchJob.id in :jobIds 
         and  bjce.errorMessage is not null
  """
  )
  fun getErrorMessages(jobIds: List<Long>): List<JobErrorMessagesView>
  fun findAllByProjectId(projectId: Long): List<BatchJob>

  @Query(
    """
    select j from BatchJob j
    where j.id in :lockedJobIds 
      and j.status in :completedStatuses 
      and j.updatedAt < :before
  """
  )
  fun getCompletedJobs(
    lockedJobIds: Iterable<Long>,
    before: Date,
    completedStatuses: List<BatchJobStatus> = BatchJobStatus.values().filter { it.completed }
  ): List<BatchJob>

  @Query(
    """
    select j from BatchJob j
    join BatchJobChunkExecution bjce on bjce.batchJob.id = j.id
    where j.id in :jobIds
    group by j.id
    having max(bjce.updatedAt) < :before
  """
  )
  fun getStuckJobs(
    jobIds: MutableSet<Long>,
    before: Date,
  ): List<BatchJob>

  @Query(
    """
    select min(bj.createdAt), max(bj.createdAt) from BatchJob bj
      where bj.project.id = :projectId
        and bj.debouncingKey = :debouncingKey
        and bj.status = 'PENDING'
  """
  )
  fun getFirstAndLastCreatedAtByDebouncingKey(projectId: Long, debouncingKey: String): Array<Date?>?
}
