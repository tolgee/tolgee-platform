package io.tolgee.repository.machineTranslation

import io.tolgee.model.MachineTranslationCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MachineTranslationCreditBucketRepository : JpaRepository<MachineTranslationCreditBucket, Long> {
  fun findByUserAccount(userAccount: UserAccount): MachineTranslationCreditBucket?
  fun findByOrganization(organization: Organization): MachineTranslationCreditBucket?
}
