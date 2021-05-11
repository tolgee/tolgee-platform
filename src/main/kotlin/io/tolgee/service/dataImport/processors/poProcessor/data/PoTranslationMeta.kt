package io.tolgee.service.dataImport.processors.poProcessor.data

class PoTranslationMeta {
    var translatorComments: MutableList<String> = mutableListOf()
    var extractedComments: MutableList<String> = mutableListOf()
    var reference: MutableList<String> = mutableListOf()
    var flags: MutableList<String> = mutableListOf()
    var context: MutableList<String> = mutableListOf()

    override fun toString(): String {
        return """PoTranslationMeta(translatorComments=$translatorComments, 
            |extractedComments=$extractedComments, reference=$reference, flags=$flags)""".trimMargin()
    }
}
