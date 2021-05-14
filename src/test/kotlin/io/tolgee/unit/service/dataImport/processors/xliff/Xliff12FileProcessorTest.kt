package io.tolgee.unit.service.dataImport.processors.xliff

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.xliff.Xliff12FileProcessor
import org.mockito.kotlin.mock
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class Xliff12FileProcessorTest {
    private lateinit var importMock: Import
    private lateinit var importFile: ImportFile
    private lateinit var importFileDto: ImportFileDto
    private lateinit var fileProcessorContext: FileProcessorContext
    private lateinit var document: Document
    private lateinit var documentBuilder: DocumentBuilder

    @BeforeMethod
    fun setup() {
        importMock = mock()
        importFile = ImportFile("exmample.xliff", importMock)
        importFileDto = ImportFileDto(
                "exmample.xliff",
                File("src/test/resources/import/xliff/example.xliff")
                        .inputStream()
        )
        fileProcessorContext = FileProcessorContext(importFileDto, importFile, mock())
        val builderFactory = DocumentBuilderFactory.newInstance()
        documentBuilder = builderFactory.newDocumentBuilder()
        document = documentBuilder.parse(fileProcessorContext.file.inputStream)
    }

    @Test
    fun `processes xliff 12 file correctly`() {
        Xliff12FileProcessor(fileProcessorContext, document).process()
        assertThat(fileProcessorContext.languages).hasSize(2)
        assertThat(fileProcessorContext.translations).hasSize(176)
        assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].text).isEqualTo("Remove %1")
        assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].language.name).isEqualTo("en")
        assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].text).isEqualTo("Eliminar %1")
        assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].language.name).isEqualTo("es-MX")
    }

    @Test
    fun `handles errors correctly`() {
        importFileDto = ImportFileDto(
                "exmample.xliff",
                File("src/test/resources/import/xliff/error_example.xliff")
                        .inputStream()
        )
        fileProcessorContext = FileProcessorContext(importFileDto, importFile, mock())
        document = documentBuilder.parse(fileProcessorContext.file.inputStream)
        Xliff12FileProcessor(fileProcessorContext, document).process()
        assertThat(fileProcessorContext.translations).hasSize(2)
        fileProcessorContext.fileEntity.issues.let { issues ->
            assertThat(issues).hasSize(4)
            assertThat(issues[0].type).isEqualTo(FileIssueType.TARGET_NOT_PROVIDED)
            assertThat(issues[0].params!![0].type).isEqualTo(FileIssueParamType.KEY_NAME)
            assertThat(issues[0].params!![0].value).isEqualTo("vpn.main.back")
            assertThat(issues[1].type).isEqualTo(FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED)
            assertThat(issues[1].params!![0].type).isEqualTo(FileIssueParamType.FILE_NODE_ORIGINAL)
            assertThat(issues[1].params!![0].value).isEqualTo("../src/platforms/android/androidauthenticationview.qml")
        }

    }
}
