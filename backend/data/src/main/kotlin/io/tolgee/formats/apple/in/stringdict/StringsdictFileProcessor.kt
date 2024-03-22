import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.apple.`in`.guessLanguageFromPath
import io.tolgee.formats.apple.`in`.guessNamespaceFromPath
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement

open class StringsdictFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  enum class ParseState {
    Initial,
    InitialDict,
    Format,
    FormatValues,
    TranslationKey,
    FormatValueTypeKey,
    PluralForm,
  }

  var state = ParseState.Initial
  private var translationKey: String = ""

  private val xmlInputFactory = XMLInputFactory.newInstance()
  private val eventReader = xmlInputFactory.createXMLEventReader(context.file.data.inputStream())
  private var formatValueTypeKey = "li"
  private val forms = mutableMapOf<String, String>()
  private var pluralForm = "other"

  override fun process() {
    try {
      while (eventReader.hasNext()) {
        val event = eventReader.nextEvent()

        if (event.isStartElement) {
          handleStartElement(event.asStartElement())
        } else if (event.isEndElement) {
          handleEndElement(event.asEndElement().name.localPart)
        }
      }

      eventReader.close()
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
    context.namespace = guessNamespaceFromPath(context.file.name)
  }

  private fun handleStartElement(startElement: StartElement) {
    when (startElement.name.localPart) {
      "dict" -> handleDictStartTag()
      "key" -> handleKeyTag(eventReader.nextEvent().asCharacters().data)
      "string" -> handleStringTag(eventReader.nextEvent().asCharacters().data)
      "plist", "version" -> { // Ignore these tags
      }

      else -> {
        throw ImportCannotParseFileException(context.file.name, "unexpected element: <${startElement.name.localPart}>")
      }
    }
  }

  private fun handleDictStartTag() {
    state =
      when (state) {
        ParseState.Initial -> ParseState.InitialDict
        ParseState.TranslationKey -> ParseState.Format
        ParseState.Format -> ParseState.FormatValues
        else -> {
          throw ImportCannotParseFileException(context.file.name, "unexpected element <dict> in state: $state")
        }
      }
  }

  private fun handleKeyTag(tagValue: String) {
    when (state) {
      ParseState.InitialDict -> {
        translationKey = tagValue
        state = ParseState.TranslationKey
      }

      ParseState.FormatValues -> {
        when (tagValue) {
          "NSStringFormatValueTypeKey" -> state = ParseState.FormatValueTypeKey
          "NSStringFormatSpecTypeKey" -> {}
          else -> {
            state = ParseState.PluralForm
            pluralForm = tagValue
          }
        }
      }

      else -> { // Do nothing
      }
    }
  }

  private fun handleStringTag(value: String) {
    if (state == ParseState.FormatValueTypeKey) {
      formatValueTypeKey = value
      state = ParseState.FormatValues
      return
    }

    if (state == ParseState.PluralForm) {
      forms[pluralForm] = value
      state = ParseState.FormatValues
      return
    }
  }

  private fun handleEndElement(endElement: String) {
    when (endElement) {
      "dict" -> handleDictEndTag()
      else -> { // Do nothing
      }
    }
  }

  private fun handleDictEndTag() {
    when (state) {
      ParseState.FormatValues -> {
        addTranslation()
        state = ParseState.Format
      }

      ParseState.Format -> {
        translationKey = ""
        state = ParseState.InitialDict
      }

      ParseState.InitialDict -> {
        state = ParseState.Initial
      }

      else -> {
        // do nothing
      }
    }
  }

  private val languageName: String by lazy {
    guessLanguageFromPath(context.file.name)
  }

  private fun addTranslation() {
    val converted =
      messageConvertor.convert(
        forms,
        languageName,
        context.importSettings.convertPlaceholdersToIcu,
        context.projectIcuPlaceholdersEnabled,
      )
    context.addTranslation(
      translationKey,
      languageName,
      converted.message,
      pluralArgName = converted.pluralArgName,
      rawData = forms,
      convertedBy = importFormat,
    )
  }

  companion object {
    private val importFormat = ImportFormat.STRINGSDICT

    private val messageConvertor = importFormat.messageConvertor
  }
}
