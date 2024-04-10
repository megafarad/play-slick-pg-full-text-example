package models.daos

import models.{Document, Page, SearchResult}

import scala.concurrent.Future

trait DocumentDAO {
  def get(id: Long): Future[Option[Document]]

  def list(page: Int, pageSize: Int): Future[Page[Document]]

  def saveNewDocument(content: String): Future[Document]

  def saveExistingDocument(id: Long, content: String): Future[Document]

  def search(query: String, page: Int, pageSize: Int): Future[Page[SearchResult]]
}
