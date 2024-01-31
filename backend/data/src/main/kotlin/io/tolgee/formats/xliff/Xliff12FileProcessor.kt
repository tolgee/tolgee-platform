package io.tolgee.formats.xliff

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import java.io.StringWriter
import java.util.*
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.StartElement

class Xliff12FileProcessor(
  override val context: FileProcessorContext,
  val xmlEventReader: XMLEventReader,
) : ImportFileProcessor() {
  private val openElements = mutableListOf("xliff")
  private val currentOpenElement: String?
    get() = openElements.lastOrNull()

  private var sw = StringWriter()
  private val of: XMLOutputFactory = XMLOutputFactory.newDefaultFactory()
  private var xw: XMLEventWriter? = null

  override fun process() {
    var fileOriginal: String? = null
    var sourceLanguage: String? = null
    var targetLanguage: String? = null
    var id: String? = null
    var currentTextValue: String? = null
    var targetProvided = false
    var idx = 0

    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      when {
        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            sw = StringWriter()
            xw = of.createXMLEventWriter(sw)
          }
          (event as? StartElement)?.let { startElement ->
            openElements.add(startElement.name.localPart.lowercase(Locale.getDefault()))
            when (currentOpenElement) {
              "file" -> {
                fileOriginal =
                  startElement
                    .getAttributeByName(QName(null, "original"))?.value
                sourceLanguage =
                  startElement
                    .getAttributeByName(QName(null, "source-language"))?.value
                targetLanguage =
                  startElement
                    .getAttributeByName(QName(null, "target-language"))?.value
              }
              "trans-unit" -> {
                id = startElement.getAttributeByName(QName(null, "id"))?.value
                if (id != null && fileOriginal != null) {
                  context.addKeyCodeReference(id!!, fileOriginal!!, null)
                }
                if (fileOriginal != null && id == null) {
                  context.fileEntity.addIssue(
                    FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
                    mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to fileOriginal!!),
                  )
                }
              }
            }
          }
        }
        event.isCharacters -> {
          if (currentOpenElement != null) {
            when (currentOpenElement!!) {
              in "source", "target", "note" -> {
                currentTextValue = (currentTextValue ?: "") + event.asCharacters().data
              }
            }
          }
        }
        event.isEndElement ->
          if (event.isEndElement) {
            when (currentOpenElement) {
              "file" -> {
                idx = 0
                fileOriginal = null
                sourceLanguage = null
                targetLanguage = null
              }
              "trans-unit" -> {
                idx++
                if (id != null && !targetProvided) {
                  context.fileEntity.addIssue(
                    FileIssueType.TARGET_NOT_PROVIDED,
                    mapOf(FileIssueParamType.KEY_NAME to id!!),
                  )
                  id = null
                }
                targetProvided = false
              }
              "source" -> {
                if (sourceLanguage != null && id != null) {
                  context.addTranslation(id!!, sourceLanguage!!, sw.toString(), idx)
                }
              }
              "target" -> {
                if (targetLanguage != null && id != null) {
                  targetProvided = true
                  context.addTranslation(id!!, targetLanguage!!, sw.toString(), idx)
                }
              }
              "note" -> {
                if (sw.toString().isNotBlank() && id != null) {
                  context.addKeyComment(id!!, sw.toString())
                }
              }
            }
            currentTextValue = null
            openElements.removeLast()
          }
      }
      if (isAnyToContentSaveOpen) {
        val startName = (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault())
        if (!contentSaveElements.contains(startName)) {
          xw?.add(event)
        }
      } else {
        xw?.close()
      }
    }
  }

  private val isAnyToContentSaveOpen
    get() = openElements.any { contentSaveElements.contains(it) }

  companion object {
    private val contentSaveElements = listOf("source", "target", "note")
  }
}
