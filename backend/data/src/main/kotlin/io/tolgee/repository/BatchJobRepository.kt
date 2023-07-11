package io.tolgee.repository

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.views.JobErrorMessagesView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

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
    nativeQuery = true,
    value = """
     select batch_job_chunk_execution.batch_job_id, sum(jsonb_array_length(success_targets)) 
      from batch_job_chunk_execution 
      where batch_job_id in :jobIds
      group by batch_job_chunk_execution.batch_job_id
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
}
