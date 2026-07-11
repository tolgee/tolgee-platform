package io.tolgee.fixtures

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.util.Collections

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Parses a MimeMessage and stores the individual parts such a plain text,
 * HTML text and attachments.
 *
 * @since 1.3
 */
open class MimeMessageParser(
  message: jakarta.mail.internet.MimeMessage,
) {
  /** The MimeMessage to convert  */
  private val mimeMessage: jakarta.mail.internet.MimeMessage

  /** @return Returns the plainContent if any
   Plain mail content from MimeMessage  */
  var plainContent: String? = null
    private set

  /** @return Returns the htmlContent if any
   Html mail content from MimeMessage  */
  var htmlContent: String? = null
    private set

  /** List of attachments of MimeMessage  */
  val attachmentList: MutableList<jakarta.activation.DataSource>

  /** Attachments stored by their content-id  */
  private val cidMap: MutableMap<String, jakarta.activation.DataSource>

  /** @return Returns the isMultiPart.
   Is this a Multipart email  */
  var isMultipart: Boolean
    private set

  /**
   * Constructs an instance with the MimeMessage to be extracted.
   *
   * @param message the message to parse
   */
  init {
    attachmentList = ArrayList<jakarta.activation.DataSource>()
    cidMap = HashMap<String, jakarta.activation.DataSource>()
    mimeMessage = message
    isMultipart = false
  }

  /**
   * Does the actual extraction.
   *
   * @return this instance
   * @throws Exception parsing the mime message failed
   */
  @Throws(Exception::class)
  fun parse(): MimeMessageParser {
    this.parse(null, mimeMessage)
    return this
  }

  @get:Throws(Exception::class)
  val to: List<Any>
    /**
     * @return the 'to' recipients of the message
     * @throws Exception determining the recipients failed
     */
    get() {
      val recipients: Array<jakarta.mail.Address> =
        mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)
      return if (recipients != null) listOf(*recipients) else ArrayList<jakarta.mail.Address>()
    }

  @get:Throws(Exception::class)
  val cc: List<Any>
    /**
     * @return the 'cc' recipients of the message
     * @throws Exception determining the recipients failed
     */
    get() {
      val recipients: Array<jakarta.mail.Address> =
        mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.CC)
      return if (recipients != null) listOf(*recipients) else ArrayList<jakarta.mail.Address>()
    }

  @get:Throws(Exception::class)
  val bcc: List<Any>
    /**
     * @return the 'bcc' recipients of the message
     * @throws Exception determining the recipients failed
     */
    get() {
      val recipients: Array<jakarta.mail.Address> =
        mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.BCC)
      return if (recipients != null) listOf(*recipients) else ArrayList<jakarta.mail.Address>()
    }

  @get:Throws(Exception::class)
  val from: String?
    /**
     * @return the 'from' field of the message
     * @throws Exception parsing the mime message failed
     */
    get() {
      val addresses: Array<jakarta.mail.Address> = mimeMessage.from
      return if (addresses == null || addresses.size == 0) {
        null
      } else {
        (addresses[0] as jakarta.mail.internet.InternetAddress).address
      }
    }

  @get:Throws(Exception::class)
  val replyTo: String?
    /**
     * @return the 'replyTo' address of the email
     * @throws Exception parsing the mime message failed
     */
    get() {
      val addresses: Array<jakarta.mail.Address> = mimeMessage.replyTo
      return if (addresses == null || addresses.size == 0) {
        null
      } else {
        (addresses[0] as jakarta.mail.internet.InternetAddress).address
      }
    }

  @get:Throws(Exception::class)
  val subject: String
    /**
     * @return the mail subject
     * @throws Exception parsing the mime message failed
     */
    get() = mimeMessage.subject

  /**
   * Extracts the content of a MimeMessage recursively.
   *
   * @param parent the parent multi-part
   * @param part   the current MimePart
   * @throws MessagingException parsing the MimeMessage failed
   * @throws IOException        parsing the MimeMessage failed
   */
  @Throws(jakarta.mail.MessagingException::class, IOException::class)
  protected fun parse(
    parent: jakarta.mail.Multipart?,
    part: jakarta.mail.internet.MimePart,
  ) {
    if (isMimeType(
        part,
        "text/plain",
      ) &&
      plainContent == null &&
      !jakarta.mail.Part.ATTACHMENT
        .equals(part.disposition, ignoreCase = true)
    ) {
      plainContent = part.content.toString()
    } else {
      if (isMimeType(
          part,
          "text/html",
        ) &&
        htmlContent == null &&
        !jakarta.mail.Part.ATTACHMENT
          .equals(part.disposition, ignoreCase = true)
      ) {
        htmlContent = part.content.toString()
      } else {
        if (isMimeType(part, "multipart/*")) {
          isMultipart = true
          val mp: jakarta.mail.Multipart = part.content as jakarta.mail.Multipart
          val count: Int = mp.count

          // iterate over all MimeBodyPart
          for (i in 0 until count) {
            parse(mp, mp.getBodyPart(i) as jakarta.mail.internet.MimeBodyPart)
          }
        } else {
          val cid = stripContentId(part.contentID)
          val ds: jakarta.activation.DataSource = createDataSource(parent, part)
          if (cid != null) {
            cidMap[cid] = ds
          }
          attachmentList.add(ds)
        }
      }
    }
  }

  /**
   * Strips the content id of any whitespace and angle brackets.
   * @param contentId the string to strip
   * @return a stripped version of the content id
   */
  private fun stripContentId(contentId: String?): String? {
    return contentId?.trim { it <= ' ' }?.replace("[\\<\\>]".toRegex(), "")
  }

  /**
   * Checks whether the MimePart contains an object of the given mime type.
   *
   * @param part     the current MimePart
   * @param mimeType the mime type to check
   * @return `true` if the MimePart matches the given mime type, `false` otherwise
   * @throws MessagingException parsing the MimeMessage failed
   * @throws IOException        parsing the MimeMessage failed
   */
  @Throws(jakarta.mail.MessagingException::class, IOException::class)
  private fun isMimeType(
    part: jakarta.mail.internet.MimePart,
    mimeType: String,
  ): Boolean {
    // Do not use part.isMimeType(String) as it is broken for MimeBodyPart
    // and does not really check the actual content type.
    return try {
      val ct: jakarta.mail.internet.ContentType =
        jakarta.mail.internet.ContentType(part.dataHandler.contentType)
      ct.match(mimeType)
    } catch (ex: jakarta.mail.internet.ParseException) {
      part.contentType.equals(mimeType, ignoreCase = true)
    }
  }

  /**
   * Parses the MimePart to create a DataSource.
   *
   * @param parent the parent multi-part
   * @param part   the current part to be processed
   * @return the DataSource
   * @throws MessagingException creating the DataSource failed
   * @throws IOException        creating the DataSource failed
   */
  @Throws(jakarta.mail.MessagingException::class, IOException::class)
  protected fun createDataSource(
    parent: jakarta.mail.Multipart?,
    part: jakarta.mail.internet.MimePart,
  ): jakarta.activation.DataSource {
    val dataHandler: jakarta.activation.DataHandler = part.dataHandler
    val dataSource: jakarta.activation.DataSource = dataHandler.dataSource
    val contentType = getBaseMimeType(dataSource.contentType)
    var content: ByteArray
    dataSource.inputStream.use { inputStream -> content = getContent(inputStream) }
    val result: jakarta.mail.util.ByteArrayDataSource = jakarta.mail.util.ByteArrayDataSource(content, contentType)
    val dataSourceName = getDataSourceName(part, dataSource)
    result.name = dataSourceName
    return result
  }

  /** @return Returns the mimeMessage.
   */
  fun getMimeMessage(): jakarta.mail.internet.MimeMessage {
    return mimeMessage
  }

  val contentIds: Collection<String>
    /**
     * Returns a collection of all content-ids in the parsed message.
     *
     *
     * The content-ids are stripped of any angle brackets, i.e. "part1" instead
     * of "&lt;part1&gt;".
     *
     * @return the collection of content ids.
     * @since 1.3.4
     */
    get() = Collections.unmodifiableSet(cidMap.keys)

  /** @return true if a plain content is available
   */
  fun hasPlainContent(): Boolean {
    return plainContent != null
  }

  /** @return true if HTML content is available
   */
  fun hasHtmlContent(): Boolean {
    return htmlContent != null
  }

  /** @return true if attachments are available
   */
  fun hasAttachments(): Boolean {
    return !attachmentList.isEmpty()
  }

  /**
   * Find an attachment using its name.
   *
   * @param name the name of the attachment
   * @return the corresponding datasource or null if nothing was found
   */
  fun findAttachmentByName(name: String): jakarta.activation.DataSource? {
    var dataSource: jakarta.activation.DataSource
    for (element in attachmentList) {
      dataSource = element
      if (name.equals(dataSource.name, ignoreCase = true)) {
        return dataSource
      }
    }
    return null
  }

  /**
   * Find an attachment using its content-id.
   *
   *
   * The content-id must be stripped of any angle brackets,
   * i.e. "part1" instead of "&lt;part1&gt;".
   *
   * @param cid the content-id of the attachment
   * @return the corresponding datasource or null if nothing was found
   * @since 1.3.4
   */
  fun findAttachmentByCid(cid: String): jakarta.activation.DataSource? {
    return cidMap[cid]
  }

  /**
   * Determines the name of the data source if it is not already set.
   *
   * @param part the mail part
   * @param dataSource the data source
   * @return the name of the data source or `null` if no name can be determined
   * @throws MessagingException accessing the part failed
   * @throws UnsupportedEncodingException decoding the text failed
   */
  @Throws(jakarta.mail.MessagingException::class, UnsupportedEncodingException::class)
  protected fun getDataSourceName(
    part: jakarta.mail.Part,
    dataSource: jakarta.activation.DataSource,
  ): String? {
    var result: String? = dataSource.name
    if (result == null || result.isEmpty()) {
      result = part.fileName
    }
    result =
      if (result != null && !result.isEmpty()) {
        jakarta.mail.internet.MimeUtility
          .decodeText(result)
      } else {
        null
      }
    return result
  }

  /**
   * Read the content of the input stream.
   *
   * @param is the input stream to process
   * @return the content of the input stream
   * @throws IOException reading the input stream failed
   */
  @Throws(IOException::class)
  private fun getContent(`is`: InputStream): ByteArray {
    val os = ByteArrayOutputStream()
    val isReader = BufferedInputStream(`is`)
    BufferedOutputStream(os).use { osWriter ->
      var ch: Int
      while (isReader.read().also { ch = it } != -1) {
        osWriter.write(ch)
      }
      osWriter.flush()
      return os.toByteArray()
    }
  }

  /**
   * Parses the mimeType.
   *
   * @param fullMimeType the mime type from the mail api
   * @return the real mime type
   */
  private fun getBaseMimeType(fullMimeType: String): String {
    val pos = fullMimeType.indexOf(';')
    return if (pos >= 0) {
      fullMimeType.substring(0, pos)
    } else {
      fullMimeType
    }
  }
}
