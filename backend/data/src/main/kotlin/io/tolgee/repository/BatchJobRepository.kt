package io.tolgee.repository

import io.tolgee.model.batch.BatchJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, Long>
