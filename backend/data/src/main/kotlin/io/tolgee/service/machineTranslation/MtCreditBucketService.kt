package io.tolgee.service.machineTranslation

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.machineTranslation.MachineTranslationCreditBucketRepository
import io.tolgee.service.OrganizationService
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
  private val organizationService: OrganizationService
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
    val balances = getCreditBalances(bucket)
    val totalBalance = balances.creditBalance + balances.extraCreditBalance

    if (totalBalance - amount < 0) {
      throw OutOfCreditsException()
    }

    bucket.consumeSufficientCredits(amount)
    save(bucket)
  }

  private fun MtCreditBucket.consumeSufficientCredits(amount: Int) {
    if (this.credits >= amount) {
      this.credits -= amount
      return
    }

    val amountToConsumeFromExtraCredits = amount - this.credits
    this.credits = 0
    this.extraCredits -= amountToConsumeFromExtraCredits
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

  @Transactional
  fun addExtraCredits(organization: Organization, amount: Long) {
    val bucket = findOrCreateBucket(organization)
    addExtraCredits(bucket, amount)
  }

  @Transactional
  fun addExtraCredits(bucket: MtCreditBucket, amount: Long) {
    bucket.extraCredits += amount
    save(bucket)
  }

  fun save(bucket: MtCreditBucket) {
    machineTranslationCreditBucketRepository.save(bucket)
  }

  fun saveAll(buckets: Iterable<MtCreditBucket>) {
    machineTranslationCreditBucketRepository.saveAll(buckets)
  }

  fun getCreditBalances(project: Project): MtCreditBalanceDto {
    return getCreditBalances(findOrCreateBucket(project))
  }

  fun getCreditBalances(bucket: MtCreditBucket): MtCreditBalanceDto {
    refillIfItsTime(bucket)
    return MtCreditBalanceDto(
      creditBalance = bucket.credits,
      bucketSize = bucket.bucketSize,
      extraCreditBalance = bucket.extraCredits,
      refilledAt = bucket.refilled
    )
  }

  fun getCreditBalances(userAccount: UserAccount): MtCreditBalanceDto {
    return getCreditBalances(findOrCreateBucket(userAccount))
  }

  fun getCreditBalances(organization: Organization): MtCreditBalanceDto {
    return getCreditBalances(findOrCreateBucket(organization))
  }

  private fun MtCreditBucket.getNextRefillDate(): Date {
    return DateUtils.addMonths(this.refilled, 1)
  }

  fun refillBucket(bucket: MtCreditBucket) {
    refillBucket(bucket, getRefillAmount(bucket.organization))
  }

  fun refillBucket(bucket: MtCreditBucket, bucketSize: Long) {
    bucket.credits = bucketSize
    bucket.refilled = currentDateProvider.getDate()
    bucket.bucketSize = bucket.credits
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
        this.initCredits()
        save(this)
      }
  }

  private fun findOrCreateBucket(organization: Organization): MtCreditBucket {
    return machineTranslationCreditBucketRepository.findByOrganization(organization)
      ?: MtCreditBucket(organization = organization).apply {
        this.initCredits()
        save(this)
      }
  }

  private fun MtCreditBucket.initCredits() {
    credits = getRefillAmount(null)
    bucketSize = credits
  }

  fun findOrCreateBucketByOrganizationId(organizationId: Long): MtCreditBucket {
    val organization = organizationService.get(organizationId)
    return findOrCreateBucket(organization)
  }

  private fun findOrCreateBucket(project: Project): MtCreditBucket {
    return project.userOwner?.let { userAccount ->
      findOrCreateBucket(userAccount)
    } ?: project.organizationOwner?.let { organization ->
      findOrCreateBucket(organization)
    } ?: throw RuntimeException("Project has no owner")
  }
}
