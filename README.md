#HISTORY-TABLES-GENERATOR

##Intro

1.In many applications and systems,there is a need to maintain the history of tables.

2.The reasons can stem from various needs like data-availibity ,auditing,backup etc.

3.This is a library that we have curated to automate the creation of history tables based on our experiences and best practices.

4.It reflects on a existing database and generates the changelogs (yes we use Liquibase) and a changeset file encapsulating those which you can readily drop in your code.


##Motivation
1.We faced a similiar need to create history tables for quite a number of tables.

2.Handrolling them wasnt an option as it would have taken time and face it,if a computer can do a better job at things ,we must tell it to do it.

##Logic/Architecture/Requirements

1.The _HISTORY table should be inserted to whenever there is an insert, update, or delete on its audited table.

It should insert all the new values for the rows (or the final values for the rows in the case of a delete), in addition to the following:

REV_ID – an auto-incrementing primary key (not nullable)]

REV_WHO – CURRENT_USER() (not nullable)

REV_WHEN – NOW() (not nullable)

REV_WHY – @OP_DESC (not nullable; default to the empty string 

REV_WHAT – an enum of "insert", "update", "delete". (not nullable)

2.In addition, it should have all the same columns as the parent table, with the same types, except those columns should be nullable.
(This is be very relevant if we change the table definition in the future.) 

3.There should be triggers defined on UPDATE and DELETE into each _HISTORY table in order to fail those operations.
No updating or deleting of history is allowed.


H/T to [RobertFischer](https://github.com/RobertFischer) for this amazing and complete description and of course for the direction.

##Usage
1.Clone the code.

2.build the master branch by the following command:

`./gradlew clean distZip` 

3.Go the `build/distribution` folder 

4.Unzip the `.zip` file 

5.Go the `{dist}/bin` folder 

6.Ensure the `JAVA_OPTS` are set as per your local environment.It can be set in the `JAVA_OPTS` section of the `.sh` or `.bat` file.

7.Run the `History-Tables-Generator` script as per your platform


###KNOBS

 `databaseHost` : The database host
 
 `databasePort` : The database port
 
 `targetDatabaseName` : The databaseName to reflect on
 
 `databaseUsername`  :Database user
 
 `databasePassword`  : Database password
 
 `exclusion.groups`  :Comma separated list of regexes to signal the existence of history tables
 
 `skipTables`  : Comma separated list of table names for which history tables are not to be generated
 
 `parent.changeset.name` : Changelog file name
 
 `author`  :Your name
 
 `outputs.dir` : Location to generate the changelogs
 


