package org.webonise.generator.models.migrations.liquibase

import groovy.transform.Canonical
import groovy.transform.InheritConstructors

/**
 * Created by Webonise on 09/09/15.
 */
@InheritConstructors
@Canonical
class CreateTableFragment extends  CreateTableBaseFragment{
    String tableName
}
