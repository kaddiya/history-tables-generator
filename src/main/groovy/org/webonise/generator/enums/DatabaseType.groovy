package org.webonise.generator.enums

/**
 * Created by Webonise on 04/09/15.
 */
enum DatabaseType {
    MYSQL("mysql")

    String name;
    public DatabaseType(String name){
        this.name = name;
    }
    public String getName(){
        return  this.name
    }
}