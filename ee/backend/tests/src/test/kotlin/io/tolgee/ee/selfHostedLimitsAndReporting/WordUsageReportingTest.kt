package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BranchedWordCountLimitTestData
import io.tolgee.development.testDataBuilder.data.WordCountLimitTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.service.branching.BranchService
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.Duration

@SpringBootTest()
class WordUsageReportingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var usageReportingService: UsageReportingService

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var tolgeeCloudLicencingClientStub: TolgeeCloudLicencingClientStub

  @Autowired
  private lateinit var organizationStatsService: OrganizationStatsService

  @Autowired
  private lateinit var branchService: BranchService

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @MockitoBean
  @Autowired
  private lateinit var billingConfProvider: PublicBillingConfProvider

  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @BeforeEach
  fun setup() {
    tolgeeCloudLicencingClientStub.enableReporting = false
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = false))
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
    usageToReportService.delete()
  }

  @AfterEach
  fun clearSubscription() {
    // The ee_subscription row is a singleton, so a subscription left behind here limits every test
    // class that runs after this one.
    eeSubscriptionRepository.deleteAll()
  }

  @Test
  fun `it reports words usage on translation edit`() {
    testWithTestData { testData, captor ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self

      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(5)
    }
  }

  @Test
  fun `the periodic report counts the words the edit produced, not the figure stored before it`() {
    testWithTestData { testData, captor ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      val storedBeforeEdit = usageToReportService.findOrCreateUsageToReport().wordsToReport
      storedBeforeEdit.assert.isNotEqualTo(7)

      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(7))
      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isTrue()

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()

      captor.assertWords(7)
      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isFalse()
    }
  }

  @Test
  fun `it reports word usage when a project turns branching off`() {
    saveSubscription()
    val testData = BranchedWordCountLimitTestData(defaultBranchWordCount = 1, featureBranchWordCount = 5)
    testDataService.saveTestData(testData.root)

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(5)

        setUseBranching(testData.project.id, false)

        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        // The feature branch's key stops counting, so only the default branch's word remains.
        captor.assertWords(1)
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `it reports word usage when a project turns branching on`() {
    saveSubscription()
    val testData = BranchedWordCountLimitTestData(defaultBranchWordCount = 1, featureBranchWordCount = 5)
    testDataService.saveTestData(testData.root)
    setUseBranching(testData.project.id, false)

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(1)

        setUseBranching(testData.project.id, true)

        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        // The feature branch's key starts counting, and its 5 words win the per-key MAX.
        captor.assertWords(5)
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a word change made while a report is being taken is not lost`() {
    testWithTestData { testData, captor ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(7))
      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(7)

      // Raised again after the report consumed the flag: the next report must still recount.
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(9))
      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isTrue()

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(9)
    }
  }

  @Test
  fun `it reports word usage when a key is renamed, which regroups the billable set`() {
    reportingAfterBranchedKeyEdit { key ->
      keyService.edit(key, "bwcl-key-renamed", null, "feature")
    }
  }

  @Test
  fun `it reports word usage when a key moves into a namespace, which regroups the billable set`() {
    reportingAfterBranchedKeyEdit { key ->
      keyService.edit(key, key.name, "some-ns", "feature")
    }
  }

  /**
   * One name on two branches collapses to the larger translation; anything that splits the two
   * names apart makes both count, without a translation being touched.
   */
  private fun reportingAfterBranchedKeyEdit(edit: (Key) -> Unit) {
    saveSubscription()
    val testData = BranchedWordCountLimitTestData(defaultBranchWordCount = 3, featureBranchWordCount = 5)
    testDataService.saveTestData(testData.root)

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(5)

        val branchedKeyId =
          testData.root.data.projects
            .first()
            .data.keys
            .first { it.self.branch?.id == testData.featureBranch.id }
            .self.id
        executeInNewTransaction { edit(entityManager.find(Key::class.java, branchedKeyId)) }

        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(8)
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `it reports word usage when a soft-deleted key is restored`() {
    testWithTestData { testData, captor ->
      val key =
        testData.projectBuilder.data.keys
          .first()
          .self

      executeInNewTransaction { keyService.softDeleteMultiple(listOf(key.id)) }
      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)

      executeInNewTransaction { keyService.restoreKeys(listOf(key.id), testData.projectBuilder.self.id) }

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(2)
    }
  }

  private fun setUseBranching(
    projectId: Long,
    useBranching: Boolean,
  ) {
    executeInNewTransaction {
      val project = entityManager.find(Project::class.java, projectId)
      projectService.editProject(project.id, EditProjectRequest(name = project.name, useBranching = useBranching))
    }
  }

  @Test
  fun `it reports word usage when project is deleted`() {
    testWithTestData { testData, captor ->
      projectService.deleteProject(testData.projectBuilder.self.id)

      // The key and word listeners react to the same deletion event, so only one of them
      // wins the immediate deferred-reporting window. Flush the other's stored value via
      // the periodic catch-up report to get a deterministic assertion.
      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `it reports word usage when a key is soft-deleted`() {
    testWithTestData { testData, captor ->
      val key =
        testData.projectBuilder.data.keys
          .first()
          .self

      keyService.softDeleteMultiple(listOf(key.id))

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `it reports word usage when a branch is deleted`() {
    saveSubscription()
    val testData = BranchedWordCountLimitTestData(defaultBranchWordCount = 1, featureBranchWordCount = 5)
    testDataService.saveTestData(testData.root)

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(5)

        branchService.deleteBranch(testData.project.id, testData.featureBranch.id)

        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        captor.assertWords(1)
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `it reports word usage when a language is soft-deleted`() {
    testWithTestData { testData, captor ->
      languageService.deleteLanguage(testData.englishLanguage.id)

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `a translation edit inside the deferral window marks the word count dirty`() {
    withDeferralWindowOpen { testData ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))

      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isTrue()
    }
  }

  @Test
  fun `creating a key does not, even though creation lists deletedAt among its modifications`() {
    withDeferralWindowOpen { testData ->
      executeInNewTransaction {
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "no-translations-key"))
      }

      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isFalse()
    }
  }

  @Test
  fun `it reports word usage when organization is deleted`() {
    testWithTestData { testData, captor ->
      organizationService.delete(testData.projectBuilder.self.organizationOwner)

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `it does not report words on a licence that does not meter them`() {
    eeSubscriptionRepository.save(
      SelfHostedSubscriptionFixture.activeSubscription {
        includedKeys = 10
        includedSeats = 10
        keysLimit = 10
        seatsLimit = 10
      },
    )
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    currentDateProvider.move(Duration.ofDays(1))

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val translation =
          testData.projectBuilder.data.translations
            .first()
            .self
        translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))

        captor.allValues.assert.isEmpty()

        // And the periodic report must omit the field, not send a zero the instance never claimed.
        currentDateProvider.move(Duration.ofDays(1))
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "forces-a-report"))
        usageReportingService.reportIfNeeded()

        captor.allValues.assert.isNotEmpty
        parseRequestArgs(captor.lastValue)["words"].assert.isNull()
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `taking the dirty flag reads and clears it in one statement`() {
    testWithTestData { testData, _ ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(6))

      // The answer comes from the row, so a cached DTO that predates the writer cannot hide it.
      usageToReportService.takeWordsDirty().assert.isTrue()
      usageToReportService.takeWordsDirty().assert.isFalse()
    }
  }

  @Test
  fun `a recount that matches the reported figure still closes the window`() {
    testWithTestData { testData, _ ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(6))
      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()

      // A word-count-preserving edit: the recount will equal what was just reported, so nothing is
      // sent — but the count was paid for, and must not be paid for again inside the window.
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(6))
      usageToReportService
        .findOrCreateUsageToReport()
        .wordsDirty.assert
        .isTrue()

      currentDateProvider.move(Duration.ofMinutes(1))
      usageReportingService.reportIfNeeded()
      val countedAt = usageToReportService.findOrCreateUsageToReport().wordsCountedAt

      currentDateProvider.move(Duration.ofMinutes(1))
      usageReportingService.reportIfNeeded()

      usageToReportService
        .findOrCreateUsageToReport()
        .wordsCountedAt.assert
        .isEqualTo(countedAt)
    }
  }

  @Test
  fun `steady key activity does not starve word reporting`() {
    testWithTestData { testData, captor ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self
      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(6))

      // A key report advances the shared clock; words must not be gated on it.
      currentDateProvider.move(Duration.ofDays(1))
      executeInNewTransaction {
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "key-churn"))
      }
      usageReportingService.reportIfNeeded()

      captor.assertWords(6)
    }
  }

  @Test
  fun `it does not report when billing is enabled (cloud)`() {
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = true))

    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    currentDateProvider.move(Duration.ofDays(1))

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val translation =
          testData.projectBuilder.data.translations
            .first()
            .self
        translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))
        captor.allValues.assert.isEmpty()
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  private fun testWithTestData(test: (WordCountLimitTestData, KArgumentCaptor<HttpEntity<*>>) -> Unit) {
    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)

    // Creating the key/translation via the test data builder already fires the deferred
    // reporting listeners once, so move time forward past the 1-minute deferral window.
    currentDateProvider.move(Duration.ofDays(1))
    // And report once, so the cloud holds the pre-change figure and a later drop is a real change.
    usageReportingService.reportIfNeeded()

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        test(testData, captor)
      }
    }
    testDataService.cleanTestData(testData.root)
  }

  /**
   * Saving the test data already reports once, so the deferral window is open by the time the
   * block runs: inside it a word count change is recorded as a flag rather than paying for the
   * count, which is what these two cases are about.
   */
  private fun withDeferralWindowOpen(test: (WordCountLimitTestData) -> Unit) {
    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    executeInNewTransaction {
      usageToReportService.storeCurrentUsage(words = organizationStatsService.countAllWordsOnInstance())
    }
    usageToReportService.takeWordsDirty()

    test(testData)

    testDataService.cleanTestData(testData.root)
  }

  /**
   * A report is one POST carrying keys, seats and words together, so a licence server that refuses
   * the word figure would leave the other two unstored — and the identical payload would then be
   * re-sent and re-refused on every tick, freezing key and seat billing indefinitely for a reason
   * that has nothing to do with them. The instance is *designed* to reach an over-allowance state
   * (auto-upgrade, restores, content replacement), so this is a state it has to survive.
   */
  @Test
  fun `a refused word figure does not stop keys and seats being reported`() {
    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    currentDateProvider.move(Duration.ofDays(1))

    var refuseWords = false
    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      // The captor is only reachable through verify, and thenAnswer needs it to decide per request.
      lateinit var requests: KArgumentCaptor<HttpEntity<*>>
      verify { requests = captor }

      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
        if (refuseWords && parseRequestArgs(requests.lastValue)["words"] != null) {
          throw wordLimitRejection()
        }
        ""
      }

      verify {
        // Report once so the figures below are deferred rather than sent as they happen, and land
        // together in a single POST — which is the shape that lets one metric sink the others.
        usageReportingService.reportIfNeeded()

        val translation =
          testData.projectBuilder.data.translations
            .first()
            .self
        translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(7))
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "forces-a-report"))

        refuseWords = true
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()

        // The retry drops the refused figure and keeps the metrics that were never at fault.
        val retried = parseRequestArgs(captor.lastValue)
        retried["words"].assert.isNull()
        retried["keys"].assert.isNotNull

        val stored = usageToReportService.findOrCreateUsageToReport()
        stored.lastReportedKeys.assert.isEqualTo(stored.keysToReport)
        stored.lastReportedWords.assert.isNotEqualTo(7L)
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `it does not resend a word figure the licence server already refused`() {
    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    currentDateProvider.move(Duration.ofDays(1))

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      // The captor is only reachable through verify, and thenAnswer needs it to decide per request.
      lateinit var requests: KArgumentCaptor<HttpEntity<*>>
      verify { requests = captor }

      thenAnswer {
        if (parseRequestArgs(requests.lastValue)["words"] != null) {
          throw wordLimitRejection()
        }
        ""
      }

      verify {
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "forces-a-report"))
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        val postsAfterFirstReport = captor.allValues.size

        // Nothing about the word count changed, so re-offering it would just be refused again.
        keyService.create(testData.projectBuilder.self, CreateKeyDto(name = "forces-another-report"))
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()

        captor.allValues.size.assert
          .isEqualTo(postsAfterFirstReport + 1)
        parseRequestArgs(captor.lastValue)["words"].assert.isNull()
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  private fun wordLimitRejection(): HttpClientErrorException =
    HttpClientErrorException.create(
      HttpStatus.BAD_REQUEST,
      "Bad Request",
      HttpHeaders(),
      """{"code":"${Message.PLAN_WORD_LIMIT_EXCEEDED.code}","params":[500,100]}""".toByteArray(),
      null,
    )

  private fun saveSubscription() {
    eeSubscriptionRepository.save(
      SelfHostedSubscriptionFixture.wordPlan {
        isPayAsYouGo = true
        includedKeys = 10
        includedSeats = 10
        keysLimit = 10
        seatsLimit = 10
        includedWords = 100
        wordsLimit = -1
      },
    )
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertWords(words: Long) {
    val data = parseRequestArgs()
    data["words"].toString().assert.isEqualTo(words.toString())
  }

  private fun KArgumentCaptor<HttpEntity<*>>.parseRequestArgs(): Map<*, *> = parseRequestArgs(this.lastValue)

  private fun parseRequestArgs(entity: HttpEntity<*>): Map<*, *> =
    objectMapper.readValue(entity.body as String, Map::class.java)
}
