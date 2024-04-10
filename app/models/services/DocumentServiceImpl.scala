package models.services

import models.{Document, Page, SearchResult}
import models.daos.DocumentDAO

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DocumentServiceImpl @Inject() (documentDAO: DocumentDAO)(implicit ec: ExecutionContext) extends DocumentService {

  def getDocument(id: Long): Future[Option[Document]] = documentDAO.get(id)

  def listDocuments(page: Int, pageSize: Int): Future[Page[Document]] = documentDAO.list(page, pageSize)

  def saveNewDocument(content: String): Future[Document] = documentDAO.saveNewDocument(content)

  def saveExistingDocument(document: Document): Future[Document] = documentDAO.saveExistingDocument(document.id.get,
    document.content)

  def searchDocuments(query: String, page: Int, pageSize: Int): Future[Page[SearchResult]] = documentDAO
    .search(query, page, pageSize)


}
