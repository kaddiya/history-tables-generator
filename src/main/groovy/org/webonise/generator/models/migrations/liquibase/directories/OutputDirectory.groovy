package org.webonise.generator.models.migrations.liquibase.directories

import groovy.transform.Canonical

/**
 * Created by Webonise on 09/09/15.
 */
@Canonical
class OutputDirectory {
    File location
    File parentChangelogFile
    Map<String,ChangeLogOutput> outputMap;
}
