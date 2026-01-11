package io.tolgee.repository.machineTranslation

import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface MachineTranslationCreditBucketRepository : JpaRepository<MtCreditBucket, Long> {
  fun findByOrganization(organization: Organization): MtCreditBucket?
}
