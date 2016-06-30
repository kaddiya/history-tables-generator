package org.webonise.generator.models.migrations.liquibase.directories

import groovy.transform.Canonical
import org.webonise.generator.models.migrations.liquibase.Changeset
import org.webonise.generator.models.migrations.liquibase.directories.ChangeLogSQLDirectory

/**
 * Created by Webonise on 09/09/15.
 */
@Canonical
class ChangeLogOutput {
    File changeLogFileName;
    ChangeLogSQLDirectory changeLogSqlDirectory;
}
