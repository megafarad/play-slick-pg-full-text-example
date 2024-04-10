package controllers

import models.services.DocumentService
import forms.{SaveDocumentForm, SearchDocumentForm}
import models.Document
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DocumentsController @Inject()(val controllerComponents: ControllerComponents, documentService: DocumentService,
                                    messagesAction: MessagesActionBuilder)
                                   (implicit val ec: ExecutionContext) extends BaseController {

  def documents(page: Int, pageSize: Int): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    documentService.listDocuments(page, pageSize).map(documentPage => Ok(views.html.showDocuments(documentPage)))
  }

  def editDocument(documentId: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    documentService.getDocument(documentId) map {
      case Some(document) => Ok(views.html.editDocument(document))
      case None => NotFound
    }
  }

  def saveNewDocument: Action[AnyContent] = Action.async { implicit request =>
    SaveDocumentForm.form.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      data => documentService.saveNewDocument(data.content) map {
        document => Redirect(routes.DocumentsController.editDocument(document.id.get))
      }
    )
  }

  def saveExistingDocument(id: Long): Action[AnyContent] = Action.async { implicit request =>
    SaveDocumentForm.form.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      data => documentService.saveExistingDocument(Document(Some(id), data.content)) map {
        document => Ok(views.html.editDocument(document))
      }
    )

  }

  def createDocument: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.createDocument(request))
  }

  def searchDocuments: Action[AnyContent] = messagesAction.async { implicit request =>
    SearchDocumentForm.form.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      data => documentService.searchDocuments(data.query, data.page, data.pageSize) map {
        searchResults => Ok(views.html.viewSearchResults(SearchDocumentForm.form.fill(data), searchResults))
      }
    )
  }

  def searchPage: Action[AnyContent] = messagesAction { implicit request =>
    Ok(views.html.searchPage(SearchDocumentForm.form))
  }

}
