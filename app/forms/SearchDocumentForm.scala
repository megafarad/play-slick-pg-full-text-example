package forms

import play.api.data.Form
import play.api.data.Forms._

object SearchDocumentForm {

  val form: Form[Data] = Form(
    mapping(
      "query" -> nonEmptyText,
      "page" -> default(number, 0),
      "pageSize" -> default(number, 10)
    )(Data.apply)(Data.unapply))

  case class Data(query: String, page: Int, pageSize: Int)
}
