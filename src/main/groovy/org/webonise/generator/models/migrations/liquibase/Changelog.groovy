package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class Changelog {
    String name;
    List<Changeset> changesets;
    File sqlScriptDirectory;
}

