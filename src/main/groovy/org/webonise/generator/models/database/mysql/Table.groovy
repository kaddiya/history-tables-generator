package org.webonise.generator.models.database.mysql

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
@CompileStatic
class Table {
    String name
    List<Column> columns;
    List<Trigger> triggers;
}
