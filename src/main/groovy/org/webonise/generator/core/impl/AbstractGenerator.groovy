package org.webonise.generator.core.impl

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.webonise.generator.core.interfaces.IGenerator
import org.webonise.generator.enums.DatabaseType
import org.webonise.generator.factories.ConnectionStringFactory
import org.webonise.generator.factories.DatabaseDriverNameFactory
import org.webonise.generator.models.migrations.liquibase.directories.ChangeLogOutput
import org.webonise.generator.models.migrations.liquibase.directories.ChangeLogSQLDirectory
import org.webonise.generator.models.migrations.liquibase.directories.OutputDirectory

import java.sql.Connection
import java.sql.DriverManager

/**
 * Created by Webonise on 04/09/15.
 */
@Slf4j
@CompileStatic
abstract class AbstractGenerator implements IGenerator {

    protected final String DB_CONNECTION_STRING;
    protected final String TARGET_DATABASE;
    protected final String DATABASE_USER;
    protected final String DATABASE_PASSWORD;
    protected final String METADATA_DB_NAME;
    protected final DatabaseType DATABASE_TYPE;
    private  final String  DRIVER_NAME;
    Connection connection;

    protected  List<String> exclusionGroupsList;

    public AbstractGenerator(DatabaseType databasetype,String metadataDBName){

        this.DATABASE_TYPE = databasetype

        this.METADATA_DB_NAME = metadataDBName
        this.TARGET_DATABASE = System.getProperty("targetDatabaseName")
        this.DATABASE_USER = System.getProperty("databaseUsername")
        this.DATABASE_PASSWORD = System.getProperty("databasePassword")

        assert DATABASE_TYPE : "Please specify a database type"
        assert METADATA_DB_NAME : "Please specify the database to lookup metadata"
        assert DATABASE_USER  : "System property databaseUsername not specified"
        assert DATABASE_PASSWORD != null :"System property databasePassword not specified"
        assert TARGET_DATABASE : "System property targetDatabaseName not specified"

        this.DRIVER_NAME = DatabaseDriverNameFactory.getDriverName(DATABASE_TYPE)
        this.DB_CONNECTION_STRING = ConnectionStringFactory.getConnectionString(DATABASE_TYPE,METADATA_DB_NAME)

    }

    @Override
    void generateHistoryTables() {
        //TODO
    }

    @Override
    void buildExclusionGroups() {
        String exlcusionGroups = System.getProperty("exclusion.groups")
        assert  exlcusionGroups : "Exclusion groups have to be specified"
        exclusionGroupsList = Arrays.asList(exlcusionGroups.split(","))
    }


    protected List<String> getExclusionGroupsList(){
        return  this.exclusionGroupsList
    }



    List<String> extractExistingHistoryTables(List<String> masterList, List<String> exlcusionGroups) {
        List<String> existingHistoryTables =  masterList.findAll {filterExclusionGroups(exlcusionGroups,it)}
        return  existingHistoryTables;
    }

    List<String> extractTablesWithExistingHistoryTables(List<String> masterList, List<String> exlcusionGroups) {
        List<String> results =  new ArrayList<String>()
        List<String> existingHistoryTables = extractExistingHistoryTables(masterList,exlcusionGroups)
        List<String> listWithoutHistoryTables = (masterList.toSet() - existingHistoryTables.toSet()).toList()

       return listWithoutHistoryTables.findAll {checkIfAHistoryTableAlreadyExistsForTable(it,existingHistoryTables,)}
    }

    @Override
    public List<String> getListOfTablesToReflectFor(){
        //IMPLEMENTED in CHILD class
    }


    protected Connection getConnection(){
        if(connection == null){
            Class.forName(DRIVER_NAME);
            this.connection = DriverManager.getConnection(DB_CONNECTION_STRING+ "?user="+DATABASE_USER+"&password="+DATABASE_PASSWORD);
        }


        return  connection
    }

    protected void reflectForTable(String tableName){

    }

    private boolean filterExclusionGroups(List<String> exclusionGroupsList,String element){
        for(String ele : exclusionGroupsList){
            if(StringUtils.containsIgnoreCase(element,ele)){
                return true;
            }

        }
        return false;
    }

    private boolean checkIfAHistoryTableAlreadyExistsForTable(String element,List<String>historyTables){
        for(String ele : historyTables){
            if(StringUtils.containsIgnoreCase(ele,element)){
                return true;
            }

        }
        return false;
    }

    protected OutputDirectory prepareOutputsDirectory(){

        String outputDirectoryPath = System.getProperty("outputs.dir")
        String parentChangeSetname = System.getProperty("parent.changeset.name")
        assert outputDirectoryPath : "please specify the output directory"
        assert parentChangeSetname :"please name this changeset"

        List<String>tableNames = getListOfTablesToReflectFor()
        Map<String,ChangeLogOutput> outputLocationMap  = new HashMap<>();
        File outputDirectory = new File(outputDirectoryPath)
        if(outputDirectory.exists()){
            outputDirectory.deleteDir()
        }
        outputDirectory.mkdirs();
        File fragmentFile = new File(outputDirectory,parentChangeSetname+".xml")
        fragmentFile.createNewFile()
            for(String  tableName :tableNames){
                ChangeLogOutput output = prepareChangeLogsFileStructure(tableName,outputDirectory)
                outputLocationMap.put(tableName,output)
            }

        return new OutputDirectory(outputDirectory,fragmentFile,outputLocationMap)

    }

    private ChangeLogOutput prepareChangeLogsFileStructure(String tableName,File location){
            String changeLogName = tableName+"_history"
            File subdirectory = new File(location,changeLogName)
            subdirectory.mkdirs();
            File insertTriggerSql =  new File(subdirectory,tableName.toUpperCase()+"_INSERT_AUDIT_TRIGGER.sql")
            insertTriggerSql.createNewFile();
            File deleteTriggerSql =  new File(subdirectory,tableName.toUpperCase()+"_DELETE_AUDIT_TRIGGER.sql")
            deleteTriggerSql.createNewFile();
            File updateTriggerSql =  new File(subdirectory,tableName.toUpperCase()+"_UPDATE_AUDIT_TRIGGER.sql")
            updateTriggerSql.createNewFile();
            File procedureSql     =  new File(subdirectory,tableName.toUpperCase()+"_INSERT_AUDIT_PROCEDURE.sql")
            procedureSql.createNewFile();

            File errorOnUpdateOnHistoryTableTriggerSql =  new File(subdirectory,tableName.toUpperCase()+"_ERROR_ON_UPDATE_AUDIT_TRIGGER.sql")
            errorOnUpdateOnHistoryTableTriggerSql.createNewFile();
            File errorOnDeleteOnHistoryTableTriggerSql     =  new File(subdirectory,tableName.toUpperCase()+"_ERROR_ON_DELETE_AUDIT_TRIGGER.sql")
            errorOnDeleteOnHistoryTableTriggerSql.createNewFile();

            ChangeLogSQLDirectory changeLogSQLDirectory = new ChangeLogSQLDirectory(subdirectory,insertTriggerSql,deleteTriggerSql,updateTriggerSql,procedureSql,errorOnUpdateOnHistoryTableTriggerSql,errorOnDeleteOnHistoryTableTriggerSql)
            File changeLogFileName =  new File(location,changeLogName+".xml")
            changeLogFileName.createNewFile();
            ChangeLogOutput output = new ChangeLogOutput(changeLogFileName,changeLogSQLDirectory)
            return  output

    }

    protected String getAuthorName(){
        assert System.getProperty("author") : "please specify the author"
        return  System.getProperty("author")
    }

    List<String> getTablesToSkip() {
        assert System.getProperty("skipTables") : "please specify the tables to skip"
        return Arrays.asList(System.getProperty("skipTables").split(","))

    }
}
