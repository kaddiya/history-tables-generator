package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class CreateProcedureFragment {
    String catalogName
    String dbms
    String encoding
    String path;
    String procedureName;
    Boolean relativeToChangeLogFile;

}
