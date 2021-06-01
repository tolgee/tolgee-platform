package io.tolgee.controllers

import io.tolgee.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.constants.ApiScope
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Language
import org.assertj.core.api.Assertions
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ExportControllerTest : ProjectAuthControllerTest() {
    @Test
    @Transactional
    fun exportZipJson() {
        val project = dbPopulator.populate(generateUniqueString())
        commitTransaction()
        val mvcResult = performAuthGet("/api/project/" + project.id + "/export/jsonZip")
                .andExpect(MockMvcResultMatchers.status().isOk).andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
        mvcResult.response
        val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
        project.languages.forEach(Consumer { l: Language ->
            val name = l.abbreviation + ".json"
            Assertions.assertThat(fileSizes).containsKey(name)
        })
        //cleanup
        projectService.deleteRepository(project.id)
    }

    @Test
    @Transactional
    @ProjectApiKeyAuthTestMethod
    fun exportZipJsonWithApiKey() {
        projectSupplier = { dbPopulator.populate(generateUniqueString()).also { commitTransaction() } }
        val mvcResult = performRepositoryAuthGet("export/jsonZip")
                .andExpect(MockMvcResultMatchers.status().isOk).andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
        mvcResult.response
        val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
        project.languages.forEach(Consumer { l: Language ->
            val name = l.abbreviation + ".json"
            Assertions.assertThat(fileSizes).containsKey(name)
        })
        //cleanup
        projectService.deleteRepository(project.id)
    }

    @Test
    @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.KEYS_EDIT])
    fun exportZipJsonApiKeyPermissionFail() {
        performRepositoryAuthGet("export/jsonZip").andIsForbidden
    }

    private fun parseZip(responseContent: ByteArray): Map<String, Long> {
        val byteArrayInputStream = ByteArrayInputStream(responseContent)
        val zipInputStream = ZipInputStream(byteArrayInputStream)
        val result = HashMap<String, Long>()
        var nextEntry: ZipEntry?
        while (zipInputStream.nextEntry.also {
                    nextEntry = it
                } != null) {
            result[nextEntry!!.name] = nextEntry!!.size
        }
        return result
    }
}
