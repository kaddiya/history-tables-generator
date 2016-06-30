package org.webonise.generator.writers

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

import org.webonise.generator.models.database.mysql.Table
import org.webonise.generator.models.migrations.liquibase.ChangeSetMapping
import org.webonise.generator.models.migrations.liquibase.Changelog
import org.webonise.generator.models.migrations.liquibase.Changeset
import org.webonise.generator.models.migrations.liquibase.CreateProcedureFragment
import org.webonise.generator.models.migrations.liquibase.CreateTableFragment
import org.webonise.generator.models.migrations.liquibase.directories.ChangeLogOutput
import org.webonise.generator.models.migrations.liquibase.directories.OutputDirectory

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by Webonise on 10/09/15.
 */
//TODO:
/*
    1.Need to extract the constants into something more readable
 */
@Slf4j
class LiquibaseChangelogWriter {

    public writeoutParentChangelog(OutputDirectory dir) {

        File targetFile = dir.parentChangelogFile
        def builder = new groovy.xml.StreamingMarkupBuilder()
        builder.encoding = "UTF-8"
        def databaseChangeLog = {
            mkp.xmlDeclaration()

            mkp.declareNamespace('xsi': 'http://www.w3.org/2001/XMLSchema-instance')
            mkp.declareNamespace('': 'http://www.liquibase.org/xml/ns/dbchangelog')
            databaseChangeLog("xsi:schemaLocation":"http://www.liquibase.org/xml/ns/dbchangelog" +"\n" +
                    "http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd") {
                dir.outputMap.forEach({ k,v ->
                    Path pathAbsolute = Paths.get(v.changeLogFileName.path)
                    Path pathBase = Paths.get(dir.parentChangelogFile.path)
                    Path pathRelative = pathBase.relativize(pathAbsolute);
                    include(file:pathRelative.toString().replace("../",""),relativeToChangelogFile:"true")
                })
            }

        }

        targetFile.text =  XmlUtil.serialize(builder.bind(databaseChangeLog))

    }


    public void writeChangeSetToChangeLog(Changeset s ,OutputDirectory outputDirectory){

    }

    @TypeChecked
    void writeoutChangeLogs(ChangeSetMapping p, OutputDirectory outputDirectory) {
        Map<String,List<Changeset>> inputMetadatamap  = p.getMappingOfTablesToChangeset();
        Map<String,ChangeLogOutput> outputMetadatamap = outputDirectory.getOutputMap()
        inputMetadatamap.each {key,inputMetadata->
            List<Changeset> changesets = inputMetadatamap.get(key)
            ChangeLogOutput output = outputMetadatamap.get(key)
            writeOutIndividualChangelog(output,changesets)
        }

    }


    void writeOutIndividualChangelog(ChangeLogOutput output,List<Changeset>changesets){


        File targetFile = output.changeLogFileName
        def builder = new groovy.xml.StreamingMarkupBuilder()
        builder.encoding = "UTF-8"
        def databaseChangeLog = {
            mkp.xmlDeclaration()

            mkp.declareNamespace('xsi': 'http://www.w3.org/2001/XMLSchema-instance')
            mkp.declareNamespace('ext': 'http://www.liquibase.org/xml/ns/dbchangelog-ext')
            mkp.declareNamespace('': 'http://www.liquibase.org/xml/ns/dbchangelog')
            databaseChangeLog("xsi:schemaLocation":"http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd "+ "\n"+
                    "http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd")  {
               changesets.forEach({ changeSetElement ->
                    //tag database changeset
                    changeSet(author:changeSetElement.getAuthor(),id:changeSetElement.getId()){
                        if(changeSetElement != null && changeSetElement.getTagDatabaseFragment() != null) {
                            tagDatabase(tag: changeSetElement.getTagDatabaseFragment().getTag())
                        }
                        //create table changeset
                        if(changeSetElement != null && changeSetElement.getCreateTableFragment()!= null){
                            CreateTableFragment tableFragment = changeSetElement.getCreateTableFragment()
                            createTable(tableName : tableFragment.getTableName()){
                                    tableFragment.getColumns().forEach({ columnElement ->
                                       //TODO:REFACTOR THIS FUGLY code and optinally print out the autoIncrement field
                                        //USe of NULLS is making this code brittle need to have a better way of priting out only those fields that are specified
                                        if(columnElement.getAutoIncrement()) {
                                            column(name: columnElement.getName(), type: columnElement.getType(), autoIncrement: columnElement.getAutoIncrement()) {
                                                columnElement.constraints.forEach({ constraintElement ->
                                                    constraints(nullable: constraintElement.getNullable(),primaryKey :constraintElement.getPrimaryKey(),primaryKeyName:constraintElement.getPrimaryKeyName())
                                                })
                                            }
                                        }
                                        else {
                                            if(columnElement.getDefaultV()==null) {
                                                column(name: columnElement.getName(), type: columnElement.getType()) {
                                                    columnElement.constraints.forEach({ constraintElement ->
                                                        constraints(nullable: constraintElement.getNullable())
                                                    })
                                                }
                                            }
                                        }

                                        if(columnElement.getDefaultV() != null){
                                            column(name: columnElement.getName(), type: columnElement.getType(),defaultValue:columnElement.getDefaultV()) {
                                                columnElement.constraints.forEach({ constraintElement ->
                                                    constraints(nullable: constraintElement.getNullable())
                                                })
                                            }
                                        }
                                    })




                            }
                        }
                        if(changeSetElement !=null && changeSetElement.getCreateProcedureFragment()!=null ){
                            CreateProcedureFragment fragment = changeSetElement.getCreateProcedureFragment()
                            createProcedure(catalogName:fragment.getCatalogName(),
                                    dbms:fragment.getDbms(),
                                    encoding:fragment.getEncoding(),
                                    path:fragment.getPath(),
                                    procedureName:fragment.getProcedureName(),
                                    relativeToChangelogFile: fragment.getRelativeToChangeLogFile(),
                                    schemaName:System.getProperty("targetDatabaseName"))
                        }
                    }

                })
            }





        }

        targetFile.text =  XmlUtil.serialize(builder.bind(databaseChangeLog))




    }

}
