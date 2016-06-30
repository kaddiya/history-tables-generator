package org.webonise.generator.models.migrations.liquibase.directories

import groovy.transform.Canonical

/**
 * Created by Webonise on 09/09/15.
 */
@Canonical
class ChangeLogSQLDirectory {
    File location;
    File insertTriggerSQL;
    File deleteTriggerSQL;
    File updateTriggerSQL;
    File insertProcedureSQL;
    File errorOnUpdateOnHistoryTableTrigger
    File errorOnDeleteOnHistoryTableTrigger
}
