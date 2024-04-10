package models.services

import models.{Document, Page, SearchResult}

import scala.concurrent.Future

trait DocumentService {

  def getDocument(id: Long): Future[Option[Document]]

  def listDocuments(page: Int, pageSize: Int): Future[Page[Document]]

  def saveNewDocument(content: String): Future[Document]

  def saveExistingDocument(document: Document): Future[Document]

  def searchDocuments(query: String, page: Int, pageSize: Int): Future[Page[SearchResult]]
}
