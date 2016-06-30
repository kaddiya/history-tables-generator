package org.webonise.generator.core.impl

import com.google.common.collect.Tables
import com.google.inject.Inject
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.webonise.generator.dbreflection.interfaces.DBReflector
import org.webonise.generator.enums.DatabaseType
import org.webonise.generator.enums.TriggerAction
import org.webonise.generator.models.database.mysql.Column
import org.webonise.generator.models.database.mysql.Table
import org.webonise.generator.models.database.mysql.Trigger
import org.webonise.generator.models.migrations.liquibase.ChangeSetMapping
import org.webonise.generator.models.migrations.liquibase.Changelog
import org.webonise.generator.models.migrations.liquibase.Changeset
import org.webonise.generator.models.migrations.liquibase.ColumnFragment
import org.webonise.generator.models.migrations.liquibase.Constraints
import org.webonise.generator.models.migrations.liquibase.CreateProcedureFragment
import org.webonise.generator.models.migrations.liquibase.CreateTableFragment
import org.webonise.generator.models.migrations.liquibase.TagDatabaseFragment
import org.webonise.generator.models.migrations.liquibase.directories.ChangeLogOutput
import org.webonise.generator.models.migrations.liquibase.directories.OutputDirectory
import org.webonise.generator.writers.LiquibaseChangelogWriter

import java.lang.reflect.Type
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Created by Webonise on 03/09/15.
 */
@InheritConstructors
@CompileStatic
@Slf4j
class MysqlGenerator extends AbstractGenerator{

    final static String MYSQL_METADATA_SCHEMA = "INFORMATION_SCHEMA"

    final static String TRIGGER_REFLECTION_QUERY = "select TRIGGER_NAME,ACTION_TIMING,ACTION_STATEMENT,EVENT_MANIPULATION from triggers where TRIGGER_SCHEMA=? and EVENT_OBJECT_TABLE = ?"
    final static String TABLE_REFLECTION_QUERY = "select COLUMN_NAME,COLUMN_DEFAULT,IS_NULLABLE,COLUMN_TYPE from COLUMNS where TABLE_NAME = ? and TABLE_SCHEMA = ?"

    @Inject
    DBReflector mysqlDbReflector;
    @Inject
    LiquibaseChangelogWriter changelogWriter

    public MysqlGenerator(){
        super(DatabaseType.MYSQL,MYSQL_METADATA_SCHEMA)
    }


    List<String> getMasterListOfTables(){

        List<String> result = new ArrayList<String>();
        String sql = "select TABLE_NAME from TABLES where TABLE_SCHEMA=? and TABLE_TYPE like \'BASE TABLE\' and TABLE_NAME not in (\'DATABASECHANGELOG\',\'DATABASECHANGELOGLOCK\')"
        try{

            Connection connection = getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1,TARGET_DATABASE)
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                result.add(rs.getString("TABLE_NAME"))
            }
        }catch (Exception e){
            log.error("Error occured in querying the tables",e)
        }
        finally {
            //connection.close();
        }
        return  result;
    }


    public List<Table> formulateTableData(){
        List<Table> tables =  new ArrayList<Table>();
        List<String> tableNames = getListOfTablesToReflectFor();
       for(String tableName : tableNames ){
           List<Trigger> triggers = getTriggerDataFor(tableName)
                Table object = new Table(tableName,getColumnDataFor(tableName),triggers)
                tables.add(object)
       }

        return  tables;
    }



    public List<Column> getColumnDataFor(String tableName){
        List<Column>result = new ArrayList<Column>();
        try{
            Connection connection = getConnection();
            PreparedStatement stmt = connection.prepareStatement(TABLE_REFLECTION_QUERY);
            stmt.setString(1,tableName)
            stmt.setString(2,TARGET_DATABASE)
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                result.add(new Column(rs.getString("COLUMN_NAME"),rs.getString("COLUMN_DEFAULT") ,rs.getString("IS_NULLABLE"),rs.getString("COLUMN_TYPE")));

            }
        }catch (Exception e){
            log.error("Error occured in querying the tables",e)
        }
        finally {
          //  connection.close();
        }
        return result;
    }

    public List<Trigger> getTriggerDataFor(String tableName){
       List<Trigger>result = new ArrayList<Trigger>();
        Connection connection = getConnection();
        PreparedStatement stmt = connection.prepareStatement(TRIGGER_REFLECTION_QUERY);
        try{
            stmt.setString(1,TARGET_DATABASE)
            stmt.setString(2,tableName)
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                result.add(new Trigger(rs.getString("TRIGGER_NAME"),rs.getString("ACTION_TIMING"),rs.getString("ACTION_STATEMENT"),rs.getString("EVENT_MANIPULATION")))

            }
        }catch (Exception e){
            log.error("Error occured in querying the tables",e)
        }
        finally {
            stmt.close();
            //connection.close();
        }
        return result;
    }

    @Override
    public List<String> getListOfTablesToReflectFor(){
        buildExclusionGroups();
        List<String>masterList = (getMasterListOfTables().toSet() - getTablesToSkip().toSet()).toList();
        List<String>historyTables = extractExistingHistoryTables(masterList,exclusionGroupsList);
        List<String>masterListWithoutHistoryTables = (masterList.toSet() - historyTables.toSet()).toList()
        /*List<String>exsWithExistingHistory = extractTablesWithExistingHistoryTables(masterList,exclusionGroupsList);
        List<String>tablesToReflectFor = (masterListWithoutHistoryTables.toSet() - tablesWithExistingHistory.toSet()).toList()*/


        return masterListWithoutHistoryTables;
    }

    @TypeChecked
    Changeset translateTableDataIntoCreateTableChangeSet(Table table){
        String author = getAuthorName()
        String id = "CREATE_"+table.getName().toUpperCase()+ "_HISTORY"
        String dbms = DatabaseType.MYSQL.getName()
        String sql = null
        String rollback = null
        CreateProcedureFragment createProcedureFragment = null
        TagDatabaseFragment tagDatabaseFragment =  null
        if(!table.getTriggers().isEmpty()){
            log.info(" existing triggers for " + table.getName())
        }
        //create teh createTableFragment
        String historyTableName = table.getName()+"_HISTORY"
        CreateTableFragment createTableFragment = new CreateTableFragment(historyTableName)
        List<ColumnFragment>generatedColumnFragments =  new ArrayList<>();
        table.getColumns().forEach({ Column c ->
            generatedColumnFragments.add(new ColumnFragment(c.getColumnName(),Arrays.asList(new Constraints(false,null,null)),c.getColumnType(),null,null))
        })
        createTableFragment.getColumns().addAll(generatedColumnFragments)
        return new Changeset(table.getName(),author,id,dbms,sql,rollback,createTableFragment,createProcedureFragment,tagDatabaseFragment)

    }

    Changeset getTagDatabaseChangeSet(Table table){
        String author = getAuthorName()
        String id = "CREATE_"+table.getName().toUpperCase()+ "_HISTORY_TAG"
        String dbms = DatabaseType.MYSQL.getName()
        String sql = null
        String rollback = null
        CreateProcedureFragment createProcedureFragment = null
        TagDatabaseFragment tagDatabaseFragment =  new TagDatabaseFragment("BEFORE_"+id)
        //create teh createTableFragment

        return new Changeset(table.getName(),author,id,dbms,sql,rollback,null,null,tagDatabaseFragment)
    }



    Changeset translateTableDataIntoInsertProcedureChangeSet(Table table){


        String id = "INSERT_"+table.getName()+"_HISTORY_PROCEDURE"


        String rollback =null
        String sql = null

        String dbms = DatabaseType.MYSQL.getName()
        String procedureName = "INSERT_"+table.getName()+"_HISTORY_PROCEDURE"

        CreateProcedureFragment createProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8",null,procedureName,true)
        return new Changeset(null,getAuthorName(),id,dbms,rollback,sql,null,createProcedureFragment,null)
    }

    Changeset translateTableDataIntoInsertTriggerChangeSet(Table table){
        String id = "INSERT_"+table.getName()+"_HISTORY_TRIGGER"


        String rollback =null
        String sql = null

        String dbms = DatabaseType.MYSQL.getName()
        String procedureName = table.getName()+"_HISTORY_INSERT_TRIGGER"

        CreateProcedureFragment createProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8",null,procedureName,true)
        return new Changeset(null,getAuthorName(),id,dbms,rollback,sql,null,createProcedureFragment,null)

    }

    Changeset translateTableDataIntoUpdateTriggerChangeSet(Table table){
        String id = "UPDATE_"+table.getName()+"_HISTORY_TRIGGER"
        String rollback =null
        String sql = null
        String dbms = DatabaseType.MYSQL.getName()
        String procedureName = table.getName()+"_HISTORY_UPDATE_TRIGGER"

        CreateProcedureFragment createProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8","",procedureName,true)
        return new Changeset(null,getAuthorName(),id,dbms,rollback,sql,null,createProcedureFragment,null)

    }

    List<Changeset> getErrorOnModifyingHistoryTableChangeSet(Table table){

        String errorOnUpdateId = "ERROR_ON_UPDATE_"+table.getName()+"_HISTORY_TRIGGER"
        String rollback =null
        String sql = null
        String dbms = DatabaseType.MYSQL.getName()
        String errorOnUpdateProcedureName = "ERROR_ON_UPDATE"+table.getName()+"_HISTORY_UPDATE_TRIGGER"

        CreateProcedureFragment errorOnUpdatecreateProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8","",errorOnUpdateProcedureName,true)
        Changeset errorOnUpdate = new Changeset(null,getAuthorName(),errorOnUpdateId,dbms,rollback,sql,null,errorOnUpdatecreateProcedureFragment,null)

        String errorOnDeleteId = "ERROR_ON_DELETE_"+table.getName()+"_HISTORY_TRIGGER"
        String errorOnDeleteProcedureName = "ERROR_ON_DELETE"+table.getName()+"_HISTORY_UPDATE_TRIGGER"
        CreateProcedureFragment errorOnDeletecreateProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8","",errorOnDeleteProcedureName,true)
        Changeset errorOnDelete = new Changeset(null,getAuthorName(),errorOnDeleteId,dbms,rollback,sql,null,errorOnDeletecreateProcedureFragment,null)

        return Arrays.asList(errorOnUpdate,errorOnDelete)

    }

    Changeset translateTableDataIntoDeleteTriggerChangeSet(Table table){
        String id = "DELETE_"+table.getName()+"_HISTORY_TRIGGER"
        String rollback =null
        String sql = null
        String dbms = DatabaseType.MYSQL.getName();
        String procedureName = table.getName()+"_HISTORY_DELETE_TRIGGER"

        CreateProcedureFragment createProcedureFragment =  new CreateProcedureFragment("procedure",dbms,"utf8","",procedureName,true)
        return new Changeset(null,getAuthorName(),id,dbms,rollback,sql,null,createProcedureFragment,null)
    }

    List<Changeset> getChangesetsForTable(Table table){
        List<Changeset> changesets =  new ArrayList<>();
        changesets.add(getTagDatabaseChangeSet(table));
        changesets.add(translateTableDataIntoCreateTableChangeSet(table))
        changesets.add(translateTableDataIntoInsertProcedureChangeSet(table))
        changesets.add(translateTableDataIntoInsertTriggerChangeSet(table))
        changesets.add(translateTableDataIntoUpdateTriggerChangeSet(table))
        changesets.add(translateTableDataIntoDeleteTriggerChangeSet(table))
        changesets.addAll(getErrorOnModifyingHistoryTableChangeSet(table))
        return changesets
    }


    void writeoutEncapsulatedChangeLogFile(OutputDirectory directory){
        changelogWriter.writeoutParentChangelog(directory)

    }

    void convertChangeLogToChangeSet(OutputDirectory dir,Changeset changeset){
        changelogWriter.writeChangeSetToChangeLog(changeset,dir)
    }

    void writeoutChangesetsTofile(OutputDirectory dir,ChangeSetMapping mapping){
        changelogWriter.writeoutChangeLogs(mapping,dir)
    }

    @TypeChecked
     ChangeSetMapping collectChangeSetMappings(List<Table> tables){
            Map<String,List<Changeset>> mapping =  new HashMap<String,List<Changeset>>();

           for(Table t : tables){
               List<Changeset> changesets = getChangesetsForTable(t)
               mapping.put(t.getName(),changesets)
           }

            return new ChangeSetMapping(mapping)
    }

    @Override
    void generateHistoryTables() {
        List<Table> tables = formulateTableData()
        ChangeSetMapping mapping = collectChangeSetMappings(tables)
        OutputDirectory dir = prepareOutputsDirectory()
        augmentChangesetsWithSQLPath(mapping,dir)
        writeoutEncapsulatedChangeLogFile(dir)
        writeoutChangesetsTofile(dir,mapping)
        generateSQLs(tables,dir,mapping)

    }

    public String getConnectionString(){
        return this.DB_CONNECTION_STRING;
    }

    @TypeChecked
    private void generateSQLs(List<Table>tables,OutputDirectory dir,ChangeSetMapping mapping){
            tables.forEach({ Table t->
                        String tableName = t.getName()
                        Changeset createTableChangeset = mapping.getMappingOfTablesToChangeset().get(tableName).find({ element ->
                            element.getCreateTableFragment()!=null
                        })
                        ChangeLogOutput output = dir.outputMap.get(tableName)
                        generateProcedureSQLsFor(output.getChangeLogSqlDirectory().getInsertProcedureSQL(),createTableChangeset)
                        generateInsertTriggerSqlFor(output.getChangeLogSqlDirectory().getInsertTriggerSQL(),createTableChangeset)
                        generateDeleteTriggerSqlFor(output.getChangeLogSqlDirectory().getDeleteTriggerSQL(),createTableChangeset)
                        generateUpdateTriggerSqlFor(output.getChangeLogSqlDirectory().getUpdateTriggerSQL(),createTableChangeset)
                        String historyTableName = createTableChangeset.getCreateTableFragment().getTableName();

                        generateSQLToFailUpdate(output.getChangeLogSqlDirectory().getErrorOnUpdateOnHistoryTableTrigger().getPath(),historyTableName)
                        generateSQLToFailDelete(output.getChangeLogSqlDirectory().getErrorOnDeleteOnHistoryTableTrigger().getPath(),historyTableName)

            })


    }

    void generateSQLToFailUpdate(String file, String tableName) {
        new File(file).write(getTriggerSQLForFailingModificationOfHistoryTables(TriggerAction.UPDATE,tableName))
    }

    void generateSQLToFailDelete(String file,String tableName){
        new File(file).write(getTriggerSQLForFailingModificationOfHistoryTables(TriggerAction.DELETE,tableName))
    }

    @TypeChecked
    private void augmentChangesetsWithSQLPath(ChangeSetMapping mapping,OutputDirectory dir){

       mapping.mappingOfTablesToChangeset.each {tableName,changesets->
          ChangeLogOutput output = dir.getOutputMap().get(tableName)
           assert output : "Table not found to augment the SQL"
           String relativePathForInsertProcedure = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.insertProcedureSQL)
           Changeset insertProcedureChangeset = findChangeSetFor(changesets,"_HISTORY_PROCEDURE")
           insertProcedureChangeset.getCreateProcedureFragment().setPath(relativePathForInsertProcedure)

           String relativePathForInsertTrigger = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.insertTriggerSQL)
           Changeset insertTriggerChangeset = findChangeSetFor(changesets,"_HISTORY_INSERT_TRIGGER")
           insertTriggerChangeset.getCreateProcedureFragment().setPath(relativePathForInsertTrigger)

           String relativePathForDeleteTrigger = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.deleteTriggerSQL)
           Changeset deleteTriggerChangeset = findChangeSetFor(changesets,"_HISTORY_DELETE_TRIGGER")
           deleteTriggerChangeset.getCreateProcedureFragment().setPath(relativePathForDeleteTrigger)

           String relativePathForUpdateTrigger = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.updateTriggerSQL)
           Changeset updateTriggerChangeset = findChangeSetFor(changesets,"_HISTORY_UPDATE_TRIGGER")
           updateTriggerChangeset.getCreateProcedureFragment().setPath(relativePathForUpdateTrigger)

           String relativePathForErrorOnDeleteTrigger = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.getErrorOnDeleteOnHistoryTableTrigger())
           Changeset errorOndeleteTriggerChangeset = findChangeSetFor(changesets,"ERROR_ON_DELETE")
           errorOndeleteTriggerChangeset.getCreateProcedureFragment().setPath(relativePathForErrorOnDeleteTrigger)

           String relativePathForErrorOnUpdateTrigger = getRelativePath(dir.getLocation(),output.changeLogSqlDirectory.getErrorOnUpdateOnHistoryTableTrigger())
           Changeset errorOnUpdateTriggerChangeset = findChangeSetFor(changesets,"ERROR_ON_UPDATE")
           errorOnUpdateTriggerChangeset.getCreateProcedureFragment().setPath(relativePathForErrorOnUpdateTrigger)




       }
    }

    private String getRelativePath(File base,File fullyQualified){
        return  base.getPath().toURI().relativize( fullyQualified.getPath().toURI() ).toString()
    }

   private Changeset findChangeSetFor(List<Changeset>changesets,String keyWord){
        changesets.find({ x ->
            x.getCreateProcedureFragment()!=null && x.getCreateProcedureFragment().getProcedureName().contains(keyWord)
        })
    }

    @TypeChecked
    private void generateProcedureSQLsFor(File outpuFile,Changeset createTableChangeset){

        String CREATE_PROCEDURE_DECLARATION = "CREATE PROCEDURE " + "INSERT_"+createTableChangeset.getHistoryTableFor()+"_HISTORY_PROCEDURE(\n IN ";
        StringBuilder builder = new StringBuilder()
        builder.append(CREATE_PROCEDURE_DECLARATION)

        createTableChangeset.getCreateTableFragment().getColumns().forEach({ ColumnFragment t->
            if(!["REV_ID","REV_WHO","REV_WHEN","REV_WHY"].contains(t.getName())){
                builder.append(t.getName() + " " + t.getType() + ",\n")
            }


        })
        builder.replace(builder.lastIndexOf(","),builder.size(),")")
        builder.append("\n BEGIN \n INSERT  INTO "+createTableChangeset.createTableFragment.tableName +"(")

        createTableChangeset.getCreateTableFragment().getColumns().forEach({ ColumnFragment t->
            if(!["REV_ID","REV_WHO","REV_WHEN","REV_WHY"].contains(t.getName())){
                builder.append(t.getName() +",\n")
            }

        })
        builder.replace(builder.lastIndexOf(","),builder.size(),")")

        builder.append("\nVALUES( USER(),\nNOW(),\n@OP_DESC, \n")

        createTableChangeset.getCreateTableFragment().getColumns().forEach({ColumnFragment t->


            if(!["REV_ID","REV_WHO","REV_WHEN","REV_WHY"].contains(t.getName())){
                builder.append(t.getName() +",\n")
            }

        })

        builder.replace(builder.lastIndexOf(","),builder.size(),");")
        builder.append("\nEND;")

        outpuFile.write(builder.toString())

    }

    private void generateInsertTriggerSqlFor(File outpuFile,Changeset createTableChangeset){
        new File(outpuFile.getPath()).write( getSqlBodyForTrigger(TriggerAction.INSERT,createTableChangeset))

    }
    private void generateDeleteTriggerSqlFor(File outpuFile,Changeset createTableChangeset){
        new File(outpuFile.getPath()).write(getSqlBodyForTrigger(TriggerAction.DELETE, createTableChangeset))
    }
    private void generateUpdateTriggerSqlFor(File outpuFile,Changeset createTableChangeset){
        new File(outpuFile.getPath()).write( getSqlBodyForTrigger(TriggerAction.UPDATE,createTableChangeset))

    }


    private String getSqlBodyForTrigger(TriggerAction action ,Changeset createTableChangeset){

        String createTriggerDeclaration = "CREATE TRIGGER "+ createTableChangeset.getHistoryTableFor().toUpperCase()+ "_"+action+ "_TRIGGER"
        String state = action == TriggerAction.DELETE ? "OLD." :"NEW."
        StringBuilder builder = new StringBuilder();

        String actionString = action == TriggerAction.UPDATE ? "BEFORE" : "AFTER"
        builder.append(createTriggerDeclaration)
        builder.append("\n "+actionString + " "+  action +" ON \n")
        builder.append(createTableChangeset.getHistoryTableFor().toUpperCase() +"\n")
        builder.append("FOR EACH ROW \n BEGIN \n")
        builder.append("CALL INSERT_"+createTableChangeset.getHistoryTableFor()+"_HISTORY_PROCEDURE (")
        builder.append("\'"+action.name()+"\' ,")

        createTableChangeset.getCreateTableFragment().getColumns().forEach({ColumnFragment t->

            if(!["REV_ID","REV_WHO","REV_WHEN","REV_WHAT","REV_WHY"].contains(t.getName())){
                builder.append(state+t.getName() +",\n")
            }
        //who when why
        })

        builder.replace(builder.lastIndexOf(","),builder.size(),");")
        builder.append("\nEND;")

        return  builder.toString()
    }


    private String getTriggerSQLForFailingModificationOfHistoryTables(TriggerAction action ,String historyTableName){

        String createTriggerDeclaration = "CREATE TRIGGER FAIL_ON_" +action+"_"+historyTableName.toUpperCase()+ "_TRIGGER"
        StringBuilder builder = new StringBuilder();

        String actionString = "BEFORE"
        builder.append(createTriggerDeclaration)
        builder.append("\n "+actionString + " "+  action +" ON \n")
        builder.append(historyTableName.toUpperCase() +"\n")
        builder.append("FOR EACH ROW \n BEGIN \n")
        builder.append("SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = \'Modification on history tables not allowed\';")

        builder.append("\nEND;")

        return  builder.toString()

    }

}
