package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class ColumnFragment {
    String name
    List<Constraints>constraints
    String type;
    Boolean autoIncrement
    String defaultV
}


