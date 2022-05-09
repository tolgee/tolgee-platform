package io.tolgee.service.machineTranslation

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.machineTranslation.MachineTranslationCreditBucketRepository
import org.apache.commons.lang3.time.DateUtils
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class MtCreditBucketService(
  private val machineTranslationCreditBucketRepository: MachineTranslationCreditBucketRepository,
  private val machineTranslationProperties: MachineTranslationProperties,
  private val currentDateProvider: CurrentDateProvider,
  private val mtCreditBucketSizeProvider: MtBucketSizeProvider,
) {

  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun consumeCredits(project: Project, amount: Int) {
    if (machineTranslationProperties.freeCreditsAmount > -1) {
      val bucket = findOrCreateBucket(project)
      consumeCredits(bucket, amount)
    }
  }

  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun consumeCredits(bucket: MtCreditBucket, amount: Int) {
    refillIfItsTime(bucket)
    if (getCreditBalance(bucket) - amount < 0) {
      throw OutOfCreditsException()
    }
    bucket.credits -= amount
    save(bucket)
  }

  @Transactional
  fun addCredits(project: Project, amount: Int) {
    val bucket = findOrCreateBucket(project)
    addCredits(bucket, amount)
  }

  @Transactional
  fun addCredits(bucket: MtCreditBucket, amount: Int) {
    bucket.credits += amount
    refillIfItsTime(bucket)
    save(bucket)
  }

  fun save(bucket: MtCreditBucket) {
    machineTranslationCreditBucketRepository.save(bucket)
  }

  fun saveAll(buckets: Iterable<MtCreditBucket>) {
    machineTranslationCreditBucketRepository.saveAll(buckets)
  }

  fun getCreditBalance(project: Project): Long {
    return getCreditBalance(findOrCreateBucket(project))
  }

  fun getCreditBalance(bucket: MtCreditBucket): Long {
    refillIfItsTime(bucket)
    return bucket.credits
  }

  fun getCreditBalance(userAccount: UserAccount): Long {
    return getCreditBalance(findOrCreateBucket(userAccount))
  }

  fun getCreditBalance(organization: Organization): Long {
    return getCreditBalance(findOrCreateBucket(organization))
  }

  private fun MtCreditBucket.getNextRefillDate(): Date {
    return DateUtils.addMonths(this.refilled, 1)
  }

  fun refillBucket(bucket: MtCreditBucket) {
    bucket.credits = getRefillAmount(bucket.organization)
    bucket.refilled = currentDateProvider.getDate()
  }

  private fun getRefillAmount(organization: Organization?): Long {
    return mtCreditBucketSizeProvider.getSize(organization)
  }

  fun refillIfItsTime(bucket: MtCreditBucket) {
    if (bucket.getNextRefillDate() <= currentDateProvider.getDate()) {
      refillBucket(bucket)
    }
  }

  private fun findOrCreateBucket(userAccount: UserAccount): MtCreditBucket {
    return machineTranslationCreditBucketRepository.findByUserAccount(userAccount)
      ?: MtCreditBucket(userAccount = userAccount).apply {
        credits = getRefillAmount(null)
        save(this)
      }
  }

  private fun findOrCreateBucket(organization: Organization): MtCreditBucket {
    return machineTranslationCreditBucketRepository.findByOrganization(organization)
      ?: MtCreditBucket(organization = organization).apply {
        credits = getRefillAmount(organization)
        save(this)
      }
  }

  private fun findOrCreateBucket(project: Project): MtCreditBucket {
    return project.userOwner?.let { userAccount ->
      findOrCreateBucket(userAccount)
    } ?: project.organizationOwner?.let { organization ->
      findOrCreateBucket(organization)
    } ?: throw RuntimeException("Project has no owner")
  }
}
