package org.webonise.generator.models.database.mysql

import groovy.transform.Canonical
import groovy.transform.Immutable
import org.webonise.generator.models.migrations.liquibase.Constraints

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class Column {
    String columnName
    String columnDefault
    String isNullable
    String columnType
}
