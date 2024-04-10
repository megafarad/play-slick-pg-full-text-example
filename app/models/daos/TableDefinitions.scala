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
