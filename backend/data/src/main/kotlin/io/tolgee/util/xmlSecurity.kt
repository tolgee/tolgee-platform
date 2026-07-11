package io.tolgee.util

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory

/**
 * Secure XML factory creators that disable external entity processing
 * to prevent XXE (XML External Entity) injection attacks.
 */
object XmlSecurity {
  /**
   * Creates a secure [XMLInputFactory] with external entity processing disabled.
   */
  fun newSecureXmlInputFactory(): XMLInputFactory {
    val factory = XMLInputFactory.newInstance()
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
    return factory
  }

  /**
   * Creates a secure [DocumentBuilderFactory] with external entity processing disabled.
   */
  fun newSecureDocumentBuilderFactory(): DocumentBuilderFactory {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.isXIncludeAware = false
    factory.isExpandEntityReferences = false
    return factory
  }
}
