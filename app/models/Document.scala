package models

import utils.StringUtils

case class Document(id: Option[Long], content: String) {
  lazy val abbreviatedContent: String = StringUtils.ellipses(content, 100)
}
