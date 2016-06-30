package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 10/09/15.
 */
@Canonical
class ChangeSetMapping {
    Map<String,List<Changeset>> mappingOfTablesToChangeset;

}
