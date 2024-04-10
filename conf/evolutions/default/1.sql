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