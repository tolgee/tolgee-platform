package io.tolgee.model.temp

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * For filtering keys that were not successfully processed in a job,
 * we need to create a temporary table with the key ids.
 *
 * Hibernate's Criteria disallows us from executing native sql queries which is required to query the failed keys
 * from job JSONB data
 *
 * Search for `create_unsuccessful_job_keys_temp` in the schema to see how the keys are retrieved.
 */
@Table(name = "temp_unsuccessful_job_keys")
@Entity
class UnsuccessfulJobKey {
  @Id
  val keyId: Long = 0
}
