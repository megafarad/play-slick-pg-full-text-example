package models.daos

import models.{Document, Page, SearchResult}
import MyPostgresProfile.api._
import play.api.Logging
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DocumentDAOImpl @Inject() (val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DocumentDAO with DAOSlick with Logging {

  def get(id: Long): Future[Option[Document]] = {
    val query = documentTableQuery.filter(_.id === id)
    db.run(query.result.headOption)
  }

  def list(page: Int, pageSize: Int): Future[Page[Document]] = {
    val offset = page * pageSize
    val paginatedQuery = documentTableQuery.sortBy(_.id.desc).drop(offset).take(pageSize)
    getDocuments(page, pageSize, offset, paginatedQuery, documentTableQuery)
  }

  def saveNewDocument(content: String): Future[Document] = {
    val insertQuery = (documentTableQuery returning documentTableQuery.map(_.id)) += Document(None, content)
    db.run(insertQuery).flatMap(documentId => get(documentId).map(_.get))
  }

  def saveExistingDocument(id: Long, content: String): Future[Document] = {
    val document = Document(Some(id), content)
    val updateQuery = documentTableQuery.filter(_.id === id).update(document)
    db.run(updateQuery).map(_ => document)
  }

  def search(query: String, page: Int, pageSize: Int): Future[Page[SearchResult]] = {
    val offset = page * pageSize
    val totalQuery = documentTableQuery.filter(table => table.searchField @@ webSearchToTsQuery(query))
      .map(t => (t.id, t.content, tsHeadline(t.content, webSearchToTsQuery(query)),
        tsRankCD(t.searchField, webSearchToTsQuery(query))))
      .sortBy(_._4.desc)

    val paginatedQuery = totalQuery.drop(offset).take(pageSize)

    for {
      searchResults <- db.run(paginatedQuery.result) map {
        results => results.map {
          case (id, content, highlight, rank) => SearchResult(highlights = highlight, rank = rank, document = Document(Some(id), content))
        }
      }
      totalCount <- db.run(totalQuery.length.result)
    } yield Page(searchResults, page, offset, totalCount.toLong, calculateTotalPages(pageSize, totalCount.toLong))
  }

  private def getDocuments(page: Int, pageSize: Int, offset: Int, paginatedQuery: Query[DocumentTable, Document, Seq],
                           totalQuery: Query[DocumentTable, Document, Seq]) = {
    for {
      documents <- db.run(paginatedQuery.result)
      totalCount <- db.run(totalQuery.length.result)
    } yield Page(documents, page, offset, totalCount.toLong, calculateTotalPages(pageSize, totalCount.toLong))
  }

  private def calculateTotalPages(pageSize: Int, totalCount: Long) = {
    (totalCount.toFloat / pageSize).ceil.toLong
  }
}
