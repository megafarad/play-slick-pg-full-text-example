# play-slick-pg-full-text-example

An example of how to use the [tminglei Slick PostgreSQL Extensions](https://github.com/tminglei/slick-pg) for full-text 
search.

## Tech stack

* Play Framework
* Scala
* PostgreSQL
* Slick

## Key bits of code

Here's the SQL evolution script. Note that there is a trigger that updates the tsvector search_field, and that there is
an index on the search_field: 

```sql
# --- !Ups

CREATE SCHEMA app;

CREATE TABLE app.document (
  id            BIGSERIAL NOT NULL PRIMARY KEY,
  content       TEXT,
  search_field  tsvector
);

CREATE FUNCTION app.update_document_search_field() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_field := to_tsvector(NEW.content);;
    RETURN NEW;;
END;;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_update_document_search_field
BEFORE INSERT OR UPDATE ON app.document
FOR EACH ROW EXECUTE FUNCTION app.update_document_search_field();

CREATE INDEX document_result_search_idx ON app.document USING GIN(search_field);

# ---!Downs

DROP TABLE app.document;
DROP FUNCTION app.update_document_search_field();
DROP SCHEMA app;
```
A simple data model:

```scala
package models

import utils.StringUtils

case class Document(id: Option[Long], content: String) {
  lazy val abbreviatedContent: String = StringUtils.ellipses(content, 100)
}

case class Page[T](items: Seq[T], page: Int, offset: Int, total: Long, totalPageCount: Long) {
  lazy val prev: Option[Int] = Option(page - 1).filter(_ >= 0)
  lazy val next: Option[Int] = Option(page + 1).filter(_ => (offset + items.size) < total)
}

case class SearchResult(highlights: String, rank: Float, document: Document)
```

A tminglei Postgres profile:

```scala
package models.daos

import com.github.tminglei.slickpg._

trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api: MyAPI = new MyAPI { }

  trait MyAPI extends ExtPostgresAPI with ArrayImplicits
    with Date2DateTimeImplicitsDuration
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {

  }
}

object MyPostgresProfile extends MyPostgresProfile
```

A Slick table definition that uses the profile:

```scala
package models.daos

import MyPostgresProfile.api._
import com.github.tminglei.slickpg.TsVector
import models.Document
import slick.lifted.ProvenShape

trait TableDefinitions {
  class DocumentTable(tag: Tag) extends Table[Document](tag, Some("app"), "document") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def content = column[String]("content")
    def searchField = column[TsVector]("search_field")
    def * : ProvenShape[Document] = (id.?, content) <> (Document.tupled, Document.unapply)
  }

  val documentTableQuery = TableQuery[DocumentTable]
}
```

A DocumentDAO implementation. Pay attention to the `search` method:

```scala
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
    val searchResultsQuery = totalQuery.map(t => (t.id, t.content, tsHeadline(t.content, webSearchToTsQuery(query)),
        tsRankCD(t.searchField, webSearchToTsQuery(query)))).sortBy(_._4.desc)

    val paginatedQuery = searchResultsQuery.drop(offset).take(pageSize)

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
```
The rest of the code is mostly for the crude web UI.

