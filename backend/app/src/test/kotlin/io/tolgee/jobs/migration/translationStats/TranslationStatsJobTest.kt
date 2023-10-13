//package io.tolgee.jobs.migration.translationStats
//
//import io.tolgee.AbstractSpringTest
//import io.tolgee.development.testDataBuilder.data.TranslationsTestData
//import io.tolgee.repository.TranslationRepository
//import io.tolgee.testing.assertions.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.springframework.batch.core.BatchStatus
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.transaction.TestTransaction
//import org.springframework.transaction.annotation.Transactional
//
//@SpringBootTest
//class TranslationStatsJobTest : AbstractSpringTest() {
//
//  @Autowired
//  lateinit var translationsStatsUpdateJobRunner: TranslationsStatsUpdateJobRunner
//
//  @Autowired
//  lateinit var translationRepository: TranslationRepository
//
//  @Test
//  @Transactional
//  fun `it adds the stats`() {
//    prepareData(10)
//
//    val instance = translationsStatsUpdateJobRunner.run()
//
//    assertStatsAdded()
//    assertThat(instance?.status).isEqualTo(BatchStatus.COMPLETED)
//  }
//
//  @Test
//  @Transactional
//  fun `it does not run multiple times for same params`() {
//    prepareData()
//
//    // first - it really runs
//    val instance = translationsStatsUpdateJobRunner.run()
//    // nothing to migrate, no run
//    val instance2 = translationsStatsUpdateJobRunner.run()
//
//    assertThat(instance).isNotNull
//    assertThat(instance2).isNull()
//  }
//
//  @Test
//  @Transactional
//  fun `it runs again when new translation without stats is created`() {
//    val testData = prepareData()
//
//    val instance = translationsStatsUpdateJobRunner.run()
//
//    TestTransaction.start()
//    val newTranslationId = translationService.setForKey(testData.aKey, mapOf("en" to "Hellooooo!"))["en"]!!.id
//    entityManager
//      .createNativeQuery(
//        "update translation set word_count = null, character_count = null where id = $newTranslationId"
//      ).executeUpdate()
//    TestTransaction.flagForCommit()
//    TestTransaction.end()
//
//    val instance2 = translationsStatsUpdateJobRunner.run()
//    assertThat(instance2?.id).isNotEqualTo(instance?.id)
//  }
//
//  private fun assertStatsAdded() {
//    val translations = translationRepository.findAll().toMutableList()
//    translations.sortBy { it.id }
//
//    assertThat(translations)
//      .allMatch { it.wordCount != null }
//      .allMatch { it.characterCount != null }
//  }
//
//  private fun prepareData(keysToCreateCount: Long = 10): TranslationsTestData {
//    val testData = TranslationsTestData()
//    testData.generateLotOfData(keysToCreateCount)
//    testDataService.saveTestData(testData.root)
//
//    commitTransaction()
//
//    entityManager
//      .createNativeQuery("update translation set word_count = null, character_count = null")
//      .executeUpdate()
//
//    commitTransaction()
//
//    val translations = translationRepository.findAll()
//    assertThat(translations).allMatch { it.wordCount == null }.allMatch { it.characterCount == null }
//
//    TestTransaction.end()
//    return testData
//  }
//}
