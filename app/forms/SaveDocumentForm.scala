package forms

import play.api.data.Form
import play.api.data.Forms._

object SaveDocumentForm {

  val form: Form[Data] = Form(mapping("content" -> nonEmptyText)(Data.apply)(Data.unapply))

  case class Data(content: String)

}
