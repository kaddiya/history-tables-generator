package org.webonise.generator.factories

import org.webonise.generator.enums.DatabaseType

/**
 * Created by Webonise on 04/09/15.
 */
class DatabaseDriverNameFactory {

    public static String getDriverName(DatabaseType databaseType) {

        switch (databaseType) {
            case DatabaseType.MYSQL:
                return "com.mysql.jdbc.Driver"
             default:
                 throw  new IllegalStateException("The databasetype provided is not supported yet")

        }
    }
}