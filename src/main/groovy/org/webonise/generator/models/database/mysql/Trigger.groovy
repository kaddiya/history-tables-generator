package org.webonise.generator.models.database.mysql

import groovy.transform.Canonical

/**
 * Created by Webonise on 08/09/15.
 */
@Canonical
class Trigger {
    String triggerName;
    String actionTiming;
    String actionStatement;
    String eventManipulation;
}
