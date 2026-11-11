package io.tolgee.service.queryBuilders

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TranslationViewDataProviderTest : AbstractSpringTest() {
  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  @Test
  fun `returns correct page size and page meta`() {
    val testData = prepareLotOfData()
    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage, testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(
          0,
          10,
        ),
      )
    assertThat(result.content).hasSize(10)
    assertThat(result.totalElements).isGreaterThan(90)
  }

  @Test
  fun `selects languages`() {
    val testData = prepareLotOfData()
    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(
          0,
          10,
        ),
      )
    assertThat(result.content[1].translations).doesNotContainKey("de")
  }

  @Test
  fun `searches in data`() {
    val testData = prepareLotOfData()
    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(0, 10),
        params =
          GetTranslationsParams().apply {
            search = "A tr"
          },
      )
    assertThat(
      result.content
        .first()
        .translations["en"]
        ?.text,
    ).isEqualTo("A translation")
    assertThat(result.totalElements).isEqualTo(1)
  }

  @Test
  fun `returns correct comment counts`() {
    val testData = generateCommentStatesTestData()

    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage, testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(0, 10),
        params = GetTranslationsParams(),
      )

    val key = result.content.find { it.keyName == "commented_key" }!!
    assertThat(key.translations["de"]?.commentCount).isEqualTo(4)
    assertThat(key.translations["de"]?.unresolvedCommentCount).isEqualTo(2)
  }

  @Test
  fun `returns failed keys`() {
    val testData = TranslationsTestData()
    val job = testData.addFailedBatchJob()
    testDataService.saveTestData(testData.root)
    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(0, 10),
        params =
          GetTranslationsParams().apply {
            filterFailedKeysOfJob = job.id
          },
      )
    result.content.assert.hasSize(1)
    result.content[0]
      .keyName.assert
      .isEqualTo("A key")
  }

  @Test
  fun `returns failed keys of a job stored as key and language product`() {
    val testData = TranslationsTestData()
    val job = testData.addFailedProductBatchJob()
    testDataService.saveTestData(testData.root)

    try {
      getKeyNamesFailedInJob(testData, job.id).assert.containsExactly("A key")
    } finally {
      testDataService.cleanTestData(testData.root)
    }
  }

  @Test
  fun `does not return succeeded keys of a job stored as key and language product`() {
    val testData = TranslationsTestData()
    val job = testData.addFailedProductBatchJob(keySucceeded = true)
    testDataService.saveTestData(testData.root)

    try {
      getKeyNamesFailedInJob(testData, job.id).assert.isEmpty()
    } finally {
      testDataService.cleanTestData(testData.root)
    }
  }

  private fun getKeyNamesFailedInJob(
    testData: TranslationsTestData,
    jobId: Long,
  ) = translationViewDataProvider
    .getData(
      projectId = testData.project.id,
      languages =
        languageService
          .dtosFromEntities(
            listOf(testData.germanLanguage),
            testData.project.id,
          ).toSet(),
      PageRequest.of(0, 10),
      params =
        GetTranslationsParams().apply {
          filterFailedKeysOfJob = jobId
        },
    ).content
    .map { it.keyName }

  private fun generateCommentStatesTestData(): TranslationsTestData {
    val testData = TranslationsTestData()
    testData.addCommentStatesData()
    testDataService.saveTestData(testData.root)
    return testData
  }

  private fun prepareLotOfData(): TranslationsTestData {
    val testData = TranslationsTestData()
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    return testData
  }
}
