package io.tolgee.api

/** The app install a change was made through, when it was not made by a person. */
interface IProjectActivityAppModel {
  val installId: Long
  val appId: String?
  val name: String?
}
