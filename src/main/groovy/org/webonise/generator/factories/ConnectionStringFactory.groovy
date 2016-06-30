package org.webonise.generator.factories

import org.webonise.generator.enums.DatabaseType

/**
 * Created by Webonise on 04/09/15.
 */
public class ConnectionStringFactory {

    public static String getConnectionString(DatabaseType databaseType,String databaseName){

        switch (databaseType){
            case DatabaseType.MYSQL :
                return  "jdbc:mysql://"+System.getProperty("databaseHost")+":"+System.getProperty("databasePort")+"/"+databaseName
            default:
                throw  new IllegalStateException("The databasetype provided is not supported yet")
        }
    }
}
