package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class Constraints {
    Boolean nullable;
    Boolean primaryKey;
    String primaryKeyName;
}
