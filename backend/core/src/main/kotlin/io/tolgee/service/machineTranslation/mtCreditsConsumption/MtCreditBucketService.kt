package io.tolgee.service.machineTranslation.mtCreditsConsumption

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.repository.machineTranslation.MachineTranslationCreditBucketRepository
import io.tolgee.util.Logging
import io.tolgee.util.addMonths
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Service
class MtCreditBucketService(
  private val machineTranslationCreditBucketRepository: MachineTranslationCreditBucketRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val entityManager: EntityManager,
  private val machineTranslationProperties: MachineTranslationProperties,
) : Logging,
  MtCreditsService {
  override fun consumeCredits(
    organizationId: Long,
    creditsInCents: Int,
  ) {
    if (!shouldConsumeCredits()) {
      return
    }

    val bucket =
      lockingProvider.withLocking(getMtCreditBucketLockName(organizationId)) {
        tryUntilItDoesntBreakConstraint {
          executeInNewTransaction(transactionManager) {
            val bucket = findOrCreateBucketByOrganizationId(organizationId)
            consumeCredits(bucket, creditsInCents)
            bucket
          }
        }
      }

    detachBucketAfterConsumption(bucket)
  }

  @Transactional(noRollbackFor = [OutOfCreditsException::class])
  @ExperimentalTime
  override fun checkPositiveBalance(organizationId: Long) {
    if (!shouldConsumeCredits()) {
      return
    }

    lockingProvider.withLocking(getMtCreditBucketLockName(organizationId)) {
      tryUntilItDoesntBreakConstraint {
        executeInNewTransaction(transactionManager) {
          val time =
            measureTime {
              val bucket = findOrCreateBucketByOrganizationId(organizationId)
              checkPositiveBalance(bucket)
            }
          logger.debug("Checked for positive credits in $time")
        }
      }
    }
  }

  private fun shouldConsumeCredits(): Boolean {
    return machineTranslationProperties.freeCreditsAmount > -1
  }

  /**
   * Since consumption does happen in a separate transaction,
   * we need to detach the bucket from the current transaction
   *
   * Otherwise, hibernate saves the entity and credits won't be consumed
   */
  private fun detachBucketAfterConsumption(bucket: MtCreditBucket) {
    val bucketRef = entityManager.getReference(MtCreditBucket::class.java, bucket.id)
    entityManager.detach(bucketRef)
  }

  private fun getMtCreditBucketLockName(organizationId: Long) = "mt-credit-lock-$organizationId"

  private fun consumeCredits(
    bucket: MtCreditBucket,
    amount: Int,
  ) {
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

  private fun checkPositiveBalance(bucket: MtCreditBucket) {
    val totalBalance = getTotalBalance(bucket)

    if (totalBalance <= 0) {
      throw OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS)
    }
  }

  private fun MtCreditBucketService.getTotalBalance(bucket: MtCreditBucket): Long {
    val balances = getCreditBalances(bucket)
    return balances.creditBalance
  }

  private fun MtCreditBucket.consumeSufficientCredits(
    amount: Int,
    organizationId: Long,
  ) {
    logger.debug(
      "Consuming $amount credits for organization $organizationId, " +
        "credits: $credits, extraCredits: $extraCredits",
    )
    if (this.credits >= amount) {
      this.credits -= amount
      return
    }

    this.credits = 0
  }

  @Transactional
  fun addExtraCredits(
    organization: Organization,
    amount: Long,
  ) {
    val bucket = findOrCreateBucket(organization)
    addExtraCredits(bucket, amount)
  }

  @Transactional
  fun addExtraCredits(
    bucket: MtCreditBucket,
    amount: Long,
  ) {
    bucket.extraCredits += amount
    save(bucket)
  }

  fun save(bucket: MtCreditBucket) {
    machineTranslationCreditBucketRepository.save(bucket)
  }

  fun saveAll(buckets: Iterable<MtCreditBucket>) {
    machineTranslationCreditBucketRepository.saveAll(buckets)
  }

  @Transactional
  override fun getCreditBalances(organizationId: Long): MtCreditBalanceDto {
    return getCreditBalances(findOrCreateBucketByOrganizationId(organizationId))
  }

  private fun getCreditBalances(bucket: MtCreditBucket): MtCreditBalanceDto {
    refillIfItsTime(bucket)
    return MtCreditBalanceDto(
      creditBalance = bucket.credits,
      bucketSize = bucket.bucketSize,
      refilledAt = bucket.refilled,
      nextRefillAt = bucket.getNextRefillDate(),
      usedCredits = bucket.bucketSize - bucket.credits,
    )
  }

  private fun MtCreditBucket.getNextRefillDate(): Date {
    return this.refilled.addMonths(1)
  }

  fun refillBucket(bucket: MtCreditBucket) {
    refillBucket(bucket, getRefillAmount())
  }

  fun refillBucket(
    bucket: MtCreditBucket,
    bucketSize: Long,
  ) {
    bucket.credits = bucketSize
    bucket.refilled = currentDateProvider.date
    bucket.bucketSize = bucket.credits
  }

  private fun getRefillAmount(): Long {
    return machineTranslationProperties.freeCreditsAmount
  }

  private fun refillIfItsTime(bucket: MtCreditBucket) {
    if (bucket.getNextRefillDate() <= currentDateProvider.date) {
      refillBucket(bucket)
    }
  }

  private fun findOrCreateBucket(organization: Organization): MtCreditBucket {
    return tryUntilItDoesntBreakConstraint {
      machineTranslationCreditBucketRepository.findByOrganization(organization) ?: createBucket(
        organization,
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
    credits = getRefillAmount()
    bucketSize = credits
  }

  fun findOrCreateBucketByOrganizationId(organizationId: Long): MtCreditBucket {
    val organization = entityManager.getReference(Organization::class.java, organizationId)
    return findOrCreateBucket(organization)
  }
}
