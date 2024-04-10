package modules

import com.google.inject.AbstractModule
import models.daos.{DocumentDAO, DocumentDAOImpl}
import models.services.{DocumentService, DocumentServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class AppModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[DocumentDAO].to[DocumentDAOImpl]
    bind[DocumentService].to[DocumentServiceImpl]
  }
}
