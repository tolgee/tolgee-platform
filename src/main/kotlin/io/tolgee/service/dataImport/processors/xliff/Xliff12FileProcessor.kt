package io.tolgee.service.dataImport.processors.xliff

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class Xliff12FileProcessor(override val context: FileProcessorContext, private val document: Document)
    : ImportFileProcessor() {
    override fun process() {
        document.getElementsByTagName("file").asSequence().forEach { fileNode ->
            val sourceLanguage = fileNode.attributes.getNamedItem("source-language").textContent
            val targetLanguage = fileNode.attributes.getNamedItem("target-language").textContent
            val fileOriginal = fileNode.attributes.getNamedItem("original")?.textContent

            fileNode.findNodesByXPath(".//trans-unit").asSequence().forEach { transUnitNode ->
                transUnitNode.attributes.getNamedItem("id").let { keyNode ->
                    keyNode?.textContent?.let { key ->
                        transUnitNode.findNodeByXPath(".//source")?.textContent?.let { source ->
                            context.addTranslation(key, sourceLanguage, source)
                        }

                        transUnitNode.findNodeByXPath(".//note")?.textContent?.let { note ->
                            context.addKeyComment(key, note)
                        }

                        fileOriginal?.let { fileOriginal ->
                            context.addKeyCodeReference(key, fileOriginal)
                        }

                        transUnitNode.findNodeByXPath(".//target")?.textContent?.let { target ->
                            context.addTranslation(key, targetLanguage, target)
                        } ?: let {
                            context.fileEntity.addIssue(
                                    FileIssueType.TARGET_NOT_PROVIDED,
                                    mapOf(FileIssueParamType.KEY_NAME to key)
                            )
                        }

                    } ?: fileOriginal?.also {
                        context.fileEntity.addIssue(
                                FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
                                mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to fileOriginal)
                        )
                    }
                }
            }
        }
    }

    fun NodeList.asSequence(): Sequence<Node> {
        return sequence<Node> {
            (0 until this@asSequence.length).forEach {
                yield(item(it))
            }
        }
    }

    private fun Node.findNodesByXPath(expression: String): Sequence<Node> {
        val xPath = XPathFactory.newInstance().newXPath()
        return (xPath.compile(expression).evaluate(this, XPathConstants.NODESET) as NodeList).asSequence()
    }

    private fun Node.findNodeByXPath(expression: String): Node? {
        val xPath = XPathFactory.newInstance().newXPath()
        return (xPath.compile(expression).evaluate(this, XPathConstants.NODE) as Node?)
    }
}
