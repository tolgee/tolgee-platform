package io.tolgee.jobs.migration

import org.springframework.batch.core.job.JobExecution

interface MigrationJobRunner {
  fun run(): JobExecution?
}
