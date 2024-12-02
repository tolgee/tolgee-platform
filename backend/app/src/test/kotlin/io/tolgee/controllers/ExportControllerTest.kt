package io.tolgee.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Language
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ExportControllerTest : ProjectAuthControllerTest() {
  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun exportZipJson() {
    val base = dbPopulator.populate()
    commitTransaction()
    projectSupplier = { base.project }
    userAccount = base.userAccount
    val mvcResult =
      performProjectAuthGet("export/jsonZip")
        .andIsOk.andDo { obj: MvcResult -> obj.getAsyncResult(60000) }.andReturn()
    mvcResult.response
    val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
    project.languages.forEach(
      Consumer { l: Language ->
        val name = l.tag + ".json"
        Assertions.assertThat(fileSizes).containsKey(name)
      },
    )
  }

  @Test
  @Transactional
  @ProjectApiKeyAuthTestMethod
  fun exportZipJsonWithApiKey() {
    val base = dbPopulator.populate()
    commitTransaction()
    projectSupplier = { base.project }
    val mvcResult =
      performProjectAuthGet("export/jsonZip")
        .andExpect(MockMvcResultMatchers.status().isOk).andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
    val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
    project.languages.forEach(
      Consumer { l: Language ->
        val name = l.tag + ".json"
        Assertions.assertThat(fileSizes).containsKey(name)
      },
    )
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  fun exportZipJsonApiKeyPermissionFail() {
    performProjectAuthGet("export/jsonZip").andIsForbidden
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `exports only permitted langs`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.viewEnOnlyUser
    projectSupplier = { testData.project }
    val result =
      performProjectAuthGet("export/jsonZip")
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
    val fileSizes = parseZip(result.response.contentAsByteArray)
    Assertions.assertThat(fileSizes).containsOnlyKeys("en.json")
  }

  private fun parseZip(responseContent: ByteArray): Map<String, Long> {
    val byteArrayInputStream = ByteArrayInputStream(responseContent)
    val zipInputStream = ZipInputStream(byteArrayInputStream)
    val result = HashMap<String, Long>()
    var nextEntry: ZipEntry?
    while (zipInputStream.nextEntry.also {
        nextEntry = it
      } != null
    ) {
      result[nextEntry!!.name] = nextEntry!!.size
    }
    return result
  }
}
