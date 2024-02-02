import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.ios.`in`.stringdict.IOsPluralToIcuParamConvertor
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
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

  private lateinit var languageName: String
  var state = ParseState.Initial
  private var translationKey: String = ""

  private val xmlInputFactory = XMLInputFactory.newInstance()
  private val eventReader = xmlInputFactory.createXMLEventReader(context.file.data.inputStream())
  private var formatValueTypeKey = "li"
  private val forms = mutableMapOf<String, String>()
  private var pluralForm = "other"

  override fun process() {
    try {
      languageName = decideLanguage()

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
      forms[pluralForm] =
        convertMessage(value, true) {
          IOsPluralToIcuParamConvertor()
        }
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

  private fun addTranslation() {
    val translation = FormsToIcuPluralConvertor(forms).convert()
    context.addTranslation(translationKey, languageName, translation)
  }

  private fun decideLanguage(): String {
    // Here, language is detected somehow, maybe based on file name or path.
    return "en"
  }
}
