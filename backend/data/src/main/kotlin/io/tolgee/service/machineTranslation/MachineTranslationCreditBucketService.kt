package io.tolgee.service.machineTranslation

import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.MachineTranslationCreditBucket
import io.tolgee.model.Project
import io.tolgee.repository.machineTranslation.MachineTranslationCreditBucketRepository
import org.apache.commons.lang3.time.DateUtils
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class MachineTranslationCreditBucketService(
  private val machineTranslationCreditBucketRepository: MachineTranslationCreditBucketRepository,
  private val machineTranslationProperties: MachineTranslationProperties
) {

  @Transactional
  fun consumeCredits(project: Project, amount: Int) {
    if (machineTranslationProperties.freeCreditsAmount > -1) {
      val bucket = findOrCreateBucket(project)
      consumeCredits(bucket, amount)
    }
  }

  @Transactional
  fun consumeCredits(bucket: MachineTranslationCreditBucket, amount: Int) {
    refillIfItsTime(bucket)
    if (getCreditBalance(bucket) - amount < 0) {
      throw OutOfCreditsException()
    }
    bucket.credits -= amount
    save(bucket)
  }

  fun save(bucket: MachineTranslationCreditBucket) {
    machineTranslationCreditBucketRepository.save(bucket)
  }

  fun findOrCreateBucket(project: Project): MachineTranslationCreditBucket {
    return project.userOwner?.let { userAccount ->
      machineTranslationCreditBucketRepository.findByUserAccount(userAccount)
        ?: MachineTranslationCreditBucket(userAccount = userAccount).apply {
          credits = getRefillAmount()
        }
    } ?: project.organizationOwner?.let { organization ->
      machineTranslationCreditBucketRepository.findByOrganization(organization)
        ?: MachineTranslationCreditBucket(organization = organization).apply {
          credits = getRefillAmount()
        }
    } ?: throw RuntimeException("Project has no owner")
  }

  fun getCreditBalance(project: Project): Long {
    return getCreditBalance(findOrCreateBucket(project))
  }

  fun getCreditBalance(bucket: MachineTranslationCreditBucket): Long {
    refillIfItsTime(bucket)
    return bucket.credits
  }

  private fun MachineTranslationCreditBucket.getNextRefillDate(): Date {
    return DateUtils.addMonths(this.refilled, 1)
  }

  fun refillBucket(bucket: MachineTranslationCreditBucket) {
    bucket.credits = getRefillAmount()
  }

  private fun getRefillAmount(): Long {
    return machineTranslationProperties.freeCreditsAmount
  }

  fun refillIfItsTime(bucket: MachineTranslationCreditBucket) {
    if (bucket.getNextRefillDate() <= Date()) {
      refillBucket(bucket)
    }
  }
}
