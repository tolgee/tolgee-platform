package io.tolgee.batch

import io.tolgee.batch.data.BatchJobType
import io.tolgee.model.batch.BatchJob
import jakarta.persistence.EntityManager

fun EntityManager.findUserVisibleBatchJobs(): List<BatchJob> =
  createQuery("from BatchJob where type not in :internalTypes", BatchJob::class.java)
    .setParameter("internalTypes", BatchJobType.internalTypes)
    .resultList
