package io.tolgee.service

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.projectActivity.ActivityViewByRevisionsProvider
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ActivityVIewByRevisionsProviderTest : ProjectAuthControllerTest() {
  @Test
  fun `it returns limited number of modified entities`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply"
    performAuthPut(path, null).andIsOk
    val revision =
      entityManager
        .createQuery(
          "from ActivityRevision ar order by ar.id desc limit 1",
          ActivityRevision::class.java,
        ).resultList
    var views = ActivityViewByRevisionsProvider(applicationContext, revision, onlyCountInListAbove = 1).get()
    views
      .first()
      .modifications!!
      .size.assert
      .isEqualTo(2)
    views = ActivityViewByRevisionsProvider(applicationContext, revision, onlyCountInListAbove = 5).get()
    views
      .first()
      .modifications!!
      .size.assert
      .isEqualTo(7)
  }

  @Test
  fun `it includes default branch modified entities when branchId is null`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testData.addDefaultBranch()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)

    performAuthPost(
      "/v2/projects/$projectId/translations",
      mapOf(
        "key" to "default_key",
        "translations" to mapOf(testData.english.tag to "default_text"),
      ),
    ).andIsOk
    val revision =
      entityManager
        .createQuery(
          "from ActivityRevision ar order by ar.id desc limit 1",
          ActivityRevision::class.java,
        ).resultList

    val branchIds =
      entityManager
        .createQuery(
          "select ame.branchId from ActivityModifiedEntity ame where ame.activityRevision.id = :revisionId",
          java.lang.Long::class.java,
        ).setParameter("revisionId", revision.first().id)
        .resultList

    branchIds.any { it == testData.defaultBranch.id }.assert.isTrue()

    val views = ActivityViewByRevisionsProvider(applicationContext, revision, null, onlyCountInListAbove = 1).get()
    views
      .first()
      .modifications!!
      .size.assert
      .isGreaterThan(0)
  }
}
