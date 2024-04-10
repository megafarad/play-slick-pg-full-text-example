package models.daos

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

trait DAOSlick extends TableDefinitions with HasDatabaseConfigProvider[JdbcProfile]
