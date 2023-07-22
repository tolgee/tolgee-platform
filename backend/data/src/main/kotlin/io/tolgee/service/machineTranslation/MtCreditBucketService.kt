package io.tolgee.service.machineTranslation

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.events.OnConsumePayAsYouGoMtCredits
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.repository.machineTranslation.MachineTranslationCreditBucketRepository
import io.tolgee.service.organization.OrganizationService
import io.tolgee.util.Logging
import io.tolgee.util.addMonths
import io.tolgee.util.logger
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Service
class MtCreditBucketService(
  private val machineTranslationCreditBucketRepository: MachineTranslationCreditBucketRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val mtCreditBucketSizeProvider: MtBucketSizeProvider,
  private val organizationService: OrganizationService,
  private val eventPublisher: ApplicationEventPublisher
) : Logging {
  @OptIn(ExperimentalTime::class)
  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun consumeCredits(project: Project, amount: Int) {
    val time = measureTime {
      val bucket = findOrCreateBucket(project)
      consumeCredits(bucket, amount)
    }
    logger.debug("Consumed $amount credits in $time")
  }

  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun consumeCredits(bucket: MtCreditBucket, amount: Int) {
    refillIfItsTime(bucket)

    // The check for sufficient credit amount (bucket, extra credits, pay as you go spending limit)
    // is done before actual machine translation is performed,
    // so we don't need to check it here
    // if user has positive balance, but has not sufficient credits for this translation,
    // we consume the rest of their credit and set the balance to 0.
    // So next time the exception will be thrown before the operation starts
    bucket.consumeSufficientCredits(amount, bucket.organization!!.id)

    save(bucket)
  }

  @OptIn(ExperimentalTime::class)
  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun checkPositiveBalance(project: Project) {
    val time = measureTime {
      val bucket = findOrCreateBucket(project)
      checkPositiveBalance(bucket)
    }
    logger.debug("Checked for positive credits in $time")
  }

  @Transactional(dontRollbackOn = [OutOfCreditsException::class])
  fun checkPositiveBalance(bucket: MtCreditBucket) {
    val totalBalance = getTotalBalance(bucket)

    if (totalBalance <= 0) {
      if (mtCreditBucketSizeProvider.isPayAsYouGo(bucket.organization)) {
        throw OutOfCreditsException(OutOfCreditsException.Reason.SPENDING_LIMIT_EXCEEDED)
      }
      throw OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS)
    }
  }

  private fun MtCreditBucketService.getTotalBalance(bucket: MtCreditBucket): Long {
    val balances = getCreditBalances(bucket)
    val availablePayAsYouGoCredits = mtCreditBucketSizeProvider.getPayAsYouGoAvailableCredits(bucket.organization)
    return balances.creditBalance + balances.extraCreditBalance + availablePayAsYouGoCredits
  }

  private fun MtCreditBucket.consumeSufficientCredits(amount: Int, organizationId: Long) {
    if (this.credits >= amount) {
      this.credits -= amount
      return
    }

    if (this.extraCredits + this.credits >= amount) {
      val amountToConsumeFromExtraCredits = amount - this.credits
      this.credits = 0
      this.extraCredits -= amountToConsumeFromExtraCredits
      return
    }

    val amountToConsumeFromPayAsYouGo = amount - this.credits - this.extraCredits
    this.credits = 0
    this.extraCredits = 0

    eventPublisher.publishEvent(
      OnConsumePayAsYouGoMtCredits(
        this@MtCreditBucketService,
        organizationId,
        amountToConsumeFromPayAsYouGo
      )
    )
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
      refilledAt = bucket.refilled,
      nextRefillAt = bucket.getNextRefillDate()
    )
  }

  fun getCreditBalances(organization: Organization): MtCreditBalanceDto {
    return getCreditBalances(findOrCreateBucket(organization))
  }

  private fun MtCreditBucket.getNextRefillDate(): Date {
    return this.refilled.addMonths(1)
  }

  fun refillBucket(bucket: MtCreditBucket) {
    refillBucket(bucket, getRefillAmount(bucket.organization))
  }

  fun refillBucket(bucket: MtCreditBucket, bucketSize: Long) {
    bucket.credits = bucketSize
    bucket.refilled = currentDateProvider.date
    bucket.bucketSize = bucket.credits
  }

  private fun getRefillAmount(organization: Organization?): Long {
    return mtCreditBucketSizeProvider.getSize(organization)
  }

  fun refillIfItsTime(bucket: MtCreditBucket) {
    if (bucket.getNextRefillDate() <= currentDateProvider.date) {
      refillBucket(bucket)
    }
  }

  private fun findOrCreateBucket(organization: Organization): MtCreditBucket {
    return tryUntilItDoesntBreakConstraint {
      machineTranslationCreditBucketRepository.findByOrganization(organization) ?: createBucket(
        organization
      )
    }
  }

  private fun createBucket(organization: Organization): MtCreditBucket {
    return MtCreditBucket(organization = organization).apply {
      this.initCredits()
      save(this)
    }
  }

  private fun MtCreditBucket.initCredits() {
    credits = getRefillAmount(this.organization)
    bucketSize = credits
  }

  fun findOrCreateBucketByOrganizationId(organizationId: Long): MtCreditBucket {
    val organization = organizationService.get(organizationId)
    return findOrCreateBucket(organization)
  }

  private fun findOrCreateBucket(project: Project): MtCreditBucket {
    return findOrCreateBucket(project.organizationOwner)
  }
}
