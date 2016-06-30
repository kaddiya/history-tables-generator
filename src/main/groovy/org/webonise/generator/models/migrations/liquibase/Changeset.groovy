package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class Changeset {
    String historyTableFor
    String author
    String id
    String dbms
    String rollback
    String sql
    CreateTableFragment createTableFragment
    CreateProcedureFragment createProcedureFragment
    TagDatabaseFragment tagDatabaseFragment
}
