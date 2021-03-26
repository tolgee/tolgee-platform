package io.tolgee.controllers

import io.tolgee.annotations.RepositoryApiKeyAuthTestMethod
import io.tolgee.constants.ApiScope
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Language
import org.assertj.core.api.Assertions
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.util.*
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ExportControllerTest : RepositoryAuthControllerTest() {
    @Test
    fun exportZipJson() {
        val repository = dbPopulator.populate(generateUniqueString())
        commitTransaction()
        val mvcResult = performAuthGet("/api/repository/" + repository.id + "/export/jsonZip")
                .andExpect(MockMvcResultMatchers.status().isOk).andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
        mvcResult.response
        val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
        repository.languages.forEach(Consumer { l: Language ->
            val name = l.abbreviation + ".json"
            Assertions.assertThat(fileSizes).containsKey(name)
        })
        //cleanup
        repositoryService.deleteRepository(repository.id)
    }

    @Test
    @RepositoryApiKeyAuthTestMethod
    fun exportZipJsonWithApiKey() {
        repositorySupplier = { dbPopulator.populate(generateUniqueString()).also { commitTransaction() } }
        val mvcResult = performRepositoryAuthGet("export/jsonZip")
                .andExpect(MockMvcResultMatchers.status().isOk).andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
        mvcResult.response
        val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
        repository.languages.forEach(Consumer { l: Language ->
            val name = l.abbreviation + ".json"
            Assertions.assertThat(fileSizes).containsKey(name)
        })
        //cleanup
        repositoryService.deleteRepository(repository.id)
    }

    @Test
    @RepositoryApiKeyAuthTestMethod(scopes = [ApiScope.KEYS_EDIT])
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
