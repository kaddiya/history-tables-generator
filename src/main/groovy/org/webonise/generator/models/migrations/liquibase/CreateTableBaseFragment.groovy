package org.webonise.generator.models.migrations.liquibase

/**
 * Created by Webonise on 09/09/15.
 */
class CreateTableBaseFragment {

    List<ColumnFragment> columns = new ArrayList<ColumnFragment>(Arrays.asList(
           new ColumnFragment("REV_ID",Arrays.asList(
                   new Constraints(false,true,"REV_ID")
           ),"int(11)",true,null),
           new ColumnFragment("REV_WHO",Arrays.asList(
                   new Constraints(false,null,null)
           ),"varchar(255)",false,null),
           new ColumnFragment("REV_WHEN",Arrays.asList(
                   new Constraints(false,null,null)
           ),"date",false,null),
           new ColumnFragment("REV_WHY",Arrays.asList(
                   new Constraints(false,null,null)
           ),"varchar(255)",false,""),
           new ColumnFragment("REV_WHAT",Arrays.asList(
                   new Constraints(false,null,null)
           ),"ENUM('INSERT', 'UPDATE', 'DELETE')",false,null)
   ))
}
