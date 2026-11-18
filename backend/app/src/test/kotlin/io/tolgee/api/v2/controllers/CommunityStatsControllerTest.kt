package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.CommunityStatsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ProfileVisibility
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class CommunityStatsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: CommunityStatsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val wednesday = LocalDate.of(2026, 3, 4)
  private val sundayOfPreviousIsoWeek = LocalDate.of(2026, 2, 22)
  private val contributors get() = "/v2/projects/${testData.publicProject.id}/community/contributors"
  private val activity get() = "/v2/projects/${testData.publicProject.id}/community/activity"

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = Date.from(wednesday.atTime(12, 0).toInstant(ZoneOffset.UTC))
    testData = CommunityStatsTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction { testData.replayActivity(entityManager) }
    executeInNewTransaction {
      testData.moveCounterDays(entityManager, testData.bob, testData.publicProject, sundayOfPreviousIsoWeek)
    }
    userAccount = testData.carol
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `contributors carry counts, pending in this project only, epoch-millis dates and languages`() {
    performAuthGet(contributors)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(3)
          node("[0].id").isEqualTo(testData.user.id)
          node("[0].contributions").isEqualTo(6)
          node("[0].suggested").isEqualTo(0)
          node("[0].active").isEqualTo(0)
          node("[1].id").isEqualTo(testData.alice.id)
          node("[1].name").isEqualTo("Alice Adams")
          node("[1].username").isAbsent()
          node("[1].contributions").isEqualTo(5)
          node("[1].suggested").isEqualTo(4)
          node("[1].accepted").isEqualTo(2)
          node("[1].declined").isEqualTo(1)
          node("[1].active").isEqualTo(1)
          node("[1].firstContributionAt").isEqualTo(1772625600000)
          node("[1].lastContributionAt").isEqualTo(1772625600000)
          node("[1].languages") {
            isArray.hasSize(2)
            node("[0].languageId").isEqualTo(testData.czech.id)
            node("[0].contributions").isEqualTo(4)
            node("[0].suggested").isEqualTo(3)
            node("[0].accepted").isEqualTo(2)
            node("[0].declined").isEqualTo(0)
            node("[1].languageId").isEqualTo(testData.german.id)
            node("[1].contributions").isEqualTo(1)
            node("[1].declined").isEqualTo(1)
          }
          node("[2].id").isEqualTo(testData.bob.id)
          node("[2].contributions").isEqualTo(2)
          node("[2].suggested").isEqualTo(2)
          node("[2].accepted").isEqualTo(1)
          node("[2].active").isEqualTo(1)
        }
        node("page.totalElements").isEqualTo(3)
      }
  }

  @Test
  fun `range and language filter narrow totals, languages and the ranking alike`() {
    performAuthGet(
      "$contributors?from=2026-03-01&to=2026-03-04&languageIds=${testData.czech.id}",
    ).andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.user.id)
          node("[0].contributions").isEqualTo(5)
          node("[1].id").isEqualTo(testData.alice.id)
          node("[1].contributions").isEqualTo(4)
          node("[1].suggested").isEqualTo(3)
          node("[1].accepted").isEqualTo(2)
          node("[1].declined").isEqualTo(0)
          node("[1].active").isEqualTo(1)
          node("[1].languages").isArray.hasSize(1)
        }
      }
    performAuthGet("$contributors?languageIds=${testData.german.id}")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[0].contributions").isEqualTo(1)
          node("[0].suggested").isEqualTo(1)
          node("[0].active").isEqualTo(0)
          node("[1].id").isEqualTo(testData.user.id)
          node("[1].contributions").isEqualTo(1)
        }
      }
  }

  @Test
  fun `contributors sort by pending count and by last contribution date, nulls last`() {
    performAuthGet("$contributors?sort=active,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].active").isEqualTo(1)
        node("_embedded.contributors[1].active").isEqualTo(1)
        node("_embedded.contributors[2].id").isEqualTo(testData.user.id)
      }
    jdbcTemplate.update(
      "update project_contributor set last_contribution_at = ? where project_id = ? and user_id = ?",
      Date.from(sundayOfPreviousIsoWeek.atStartOfDay().toInstant(ZoneOffset.UTC)),
      testData.publicProject.id,
      testData.bob.id,
    )
    jdbcTemplate.update(
      "delete from project_contributor where project_id = ? and user_id = ?",
      testData.publicProject.id,
      testData.user.id,
    )
    performAuthGet("$contributors?sort=lastContributionAt,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[0].lastContributionAt").isEqualTo(1772625600000)
        node("_embedded.contributors[1].id").isEqualTo(testData.bob.id)
        node("_embedded.contributors[1].lastContributionAt").isEqualTo(1771718400000)
        node("_embedded.contributors[2].id").isEqualTo(testData.user.id)
        node("_embedded.contributors[2].lastContributionAt").isNull()
      }
  }

  @Test
  fun `contributors can be sorted by another counter`() {
    performAuthGet("$contributors?sort=accepted,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
  }

  @Test
  fun `withSuggestionsOnly leaves out contributors without a suggestion in the range`() {
    performAuthGet("$contributors?withSuggestionsOnly=true")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[1].id").isEqualTo(testData.bob.id)
        }
      }
    performAuthGet("$contributors?withSuggestionsOnly=true&from=2026-03-01")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
  }

  @Test
  fun `an unknown sort property falls back to the default order`() {
    performAuthGet("$contributors?sort=name,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
      }
  }

  @Test
  fun `an extreme to date is clamped instead of failing`() {
    performAuthGet("$activity?to=+999999999-12-31").andIsOk
    performAuthGet("$contributors?to=-999999999-01-01").andIsOk
  }

  @Test
  fun `the page size is capped at 100`() {
    performAuthGet("$contributors?size=2000").andIsOk.andAssertThatJson {
      node("page.size").isEqualTo(100)
    }
  }

  @Test
  fun `a single contributor is the list's entry for the same range and languages`() {
    assertAliceEqualsListEntry("from=2026-02-16")
    assertAliceEqualsListEntry("from=2026-03-01&languageIds=${testData.czech.id}")
    performAuthGet("$contributors/${testData.alice.id}?from=2026-02-16")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(5)
        node("active").isEqualTo(1)
        node("firstContributionAt").isEqualTo(1772625600000)
      }
  }

  private fun assertAliceEqualsListEntry(query: String) {
    val list = jsonOf("$contributors?$query")
    val single = jsonOf("$contributors/${testData.alice.id}?$query")
    val alice =
      list
        .path("_embedded")
        .path("contributors")
        .path(1)
    alice
      .path("id")
      .asLong()
      .assert
      .isEqualTo(testData.alice.id)
    single.assert.isEqualTo(alice)
  }

  private fun jsonOf(url: String) = performAuthGet(url).andIsOk.getContent<JsonNode>()

  @Test
  fun `a user the list does not show for the range and languages is not found`() {
    performAuthGet("$contributors/${testData.carol.id}").andIsNotFound
    performAuthGet("$contributors/${testData.dave.id}").andIsNotFound
    performAuthGet("$contributors/999999999").andIsNotFound
    performAuthGet("$contributors/${testData.bob.id}?from=2026-03-01").andIsNotFound
    performAuthGet("$contributors/${testData.bob.id}?languageIds=${testData.german.id}").andIsNotFound
  }

  @Test
  fun `a single contributor without from covers the whole history`() {
    executeInNewTransaction {
      testData.moveCounterDays(entityManager, testData.bob, testData.publicProject, LocalDate.of(2023, 6, 1))
    }
    performAuthGet("$contributors/${testData.bob.id}")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(2)
      }
    performAuthGet("$contributors/${testData.bob.id}?to=$wednesday").andIsOk
    performAuthGet("$contributors/${testData.bob.id}?from=${wednesday.minusYears(3)}").andIsNotFound
  }

  @Test
  fun `a single contributor of a private project is hidden from a non-member`() {
    userAccount = testData.bob
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors/${testData.alice.id}")
      .andIsNotFound
  }

  @Test
  fun `a private project is hidden from a non-member`() {
    userAccount = testData.bob
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors").andIsNotFound
  }

  @Test
  fun `search matches the display name ignoring case, never the e-mail, and narrows totalElements`() {
    performAuthGet("$contributors?search=ALICE&size=1")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
    val everyUsernameEndsWith = "community.test"
    performAuthGet("$contributors?search=$everyUsernameEndsWith").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `profilePublic is true for users with a counted contribution in any public project, false otherwise`() {
    userAccount = testData.user
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors?sort=suggested,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(3)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[0].profilePublic").isEqualTo(true)
          node("[1].id").isEqualTo(testData.carol.id)
          node("[1].profilePublic").isEqualTo(false)
          node("[2].id").isEqualTo(testData.user.id)
          node("[2].profilePublic").isEqualTo(true)
        }
      }
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors/${testData.carol.id}")
      .andIsOk
      .andAssertThatJson { node("profilePublic").isEqualTo(false) }
  }

  @Test
  fun `the single contributor's profilePublic follows a PRIVATE profile setting`() {
    executeInNewTransaction { testData.setProfileVisibility(entityManager, testData.alice, ProfileVisibility.PRIVATE) }
    performAuthGet("$contributors/${testData.alice.id}")
      .andIsOk
      .andAssertThatJson { node("profilePublic").isEqualTo(false) }
  }

  @Test
  fun `hasPublicContribution counts members and non-members alike, public projects only`() {
    for ((user, expected) in listOf(testData.alice to true, testData.user to true, testData.carol to false)) {
      userAccount = user
      performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
        node("hasPublicContribution").isEqualTo(expected)
      }
    }
  }

  @Test
  fun `activity returns zero-filled buckets ending with the current week and live tile counts`() {
    performAuthGet(activity)
      .andIsOk
      .andAssertThatJson {
        node("buckets") {
          isArray.hasSize(12)
          node("[0].start").isEqualTo("2025-12-15")
          node("[9].start").isEqualTo("2026-02-16")
          node("[9].contributions").isEqualTo(2)
          node("[9].suggested").isEqualTo(2)
          node("[9].accepted").isEqualTo(1)
          node("[9].declined").isEqualTo(0)
          node("[10].contributions").isEqualTo(0)
          node("[11].start").isEqualTo("2026-03-02")
          node("[11].contributions").isEqualTo(11)
          node("[11].suggested").isEqualTo(4)
          node("[11].accepted").isEqualTo(2)
          node("[11].declined").isEqualTo(1)
        }
        node("translationsWithAnySuggestion").isEqualTo(4)
        node("translationsWithPendingSuggestion").isEqualTo(2)
      }
  }

  @Test
  fun `activity respects range, daily granularity and language filter, tiles included`() {
    performAuthGet("$activity?from=2026-02-16&to=2026-02-22&granularity=DAY")
      .andIsOk
      .andAssertThatJson {
        node("buckets") {
          isArray.hasSize(7)
          node("[0].start").isEqualTo("2026-02-16")
          node("[6].start").isEqualTo("2026-02-22")
          node("[6].contributions").isEqualTo(2)
        }
        node("translationsWithAnySuggestion").isEqualTo(0)
        node("translationsWithPendingSuggestion").isEqualTo(0)
      }
    performAuthGet("$activity?languageIds=${testData.german.id}")
      .andIsOk
      .andAssertThatJson {
        node("buckets[11].contributions").isEqualTo(2)
        node("buckets[11].suggested").isEqualTo(1)
        node("buckets[11].declined").isEqualTo(1)
        node("translationsWithAnySuggestion").isEqualTo(1)
        node("translationsWithPendingSuggestion").isEqualTo(0)
      }
  }

  @Test
  fun `the range is capped at 2 years and a from after to collapses to one bucket`() {
    performAuthGet("$activity?from=2000-01-01")
      .andIsOk
      .andAssertThatJson {
        node("buckets[0].start").isEqualTo("2024-03-04")
        node("buckets").isArray.hasSize(105)
      }
    performAuthGet("$activity?from=2026-03-10&to=2026-03-04&granularity=DAY")
      .andIsOk
      .andAssertThatJson {
        node("buckets").isArray.hasSize(1)
        node("buckets[0].start").isEqualTo("2026-03-04")
      }
  }

  @Test
  fun `a soft-deleted language drops out of totals, languages and activity alike`() {
    executeInNewTransaction { testData.softDeleteGerman(entityManager) }
    performAuthGet(contributors)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
        node("_embedded.contributors[0].contributions").isEqualTo(5)
        node("_embedded.contributors[1].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[1].contributions").isEqualTo(4)
        node("_embedded.contributors[1].suggested").isEqualTo(3)
        node("_embedded.contributors[1].declined").isEqualTo(0)
        node("_embedded.contributors[1].active").isEqualTo(1)
        node("_embedded.contributors[1].languages").isArray.hasSize(1)
      }
    performAuthGet("$contributors/${testData.alice.id}")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(4)
        node("suggested").isEqualTo(3)
        node("languages").isArray.hasSize(1)
      }
    jdbcTemplate.update(
      "update translation_suggestion set state = 'ACTIVE' where language_id = ?",
      testData.german.id,
    )
    performAuthGet("$activity?from=2026-03-02")
      .andIsOk
      .andAssertThatJson {
        node("buckets[0].contributions").isEqualTo(9)
        node("buckets[0].suggested").isEqualTo(3)
        node("buckets[0].accepted").isEqualTo(2)
        node("buckets[0].declined").isEqualTo(0)
        node("translationsWithAnySuggestion").isEqualTo(3)
        node("translationsWithPendingSuggestion").isEqualTo(2)
      }
    performAuthGet("$contributors?sort=active,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].active").isEqualTo(1)
      }
  }
}
