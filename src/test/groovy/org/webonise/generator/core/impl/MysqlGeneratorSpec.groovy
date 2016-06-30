package org.webonise.generator.core.impl

import com.google.inject.Guice
import com.google.inject.Injector
import org.webonise.generator.core.interfaces.IGenerator
import org.webonise.generator.injector.GeneratorInjector
import org.webonise.generator.models.database.mysql.Table
import org.webonise.generator.models.migrations.liquibase.ChangeSetMapping
import org.webonise.generator.models.migrations.liquibase.Changeset
import org.webonise.generator.models.migrations.liquibase.directories.OutputDirectory
import spock.lang.Shared
import spock.lang.Specification

import static spock.util.matcher.HamcrestSupport.that
import static org.hamcrest.Matchers.containsInAnyOrder

/**
 * Created by Webonise on 04/09/15.
 */
class MysqlGeneratorSpec extends Specification {

   @Shared
   def Injector injector;

    def setupSpec(){
        injector =  Guice.createInjector(new GeneratorInjector())
    }

    def "MysqGenerator should not be null"(){
        when:
        IGenerator generator = injector.getInstance(MysqlGenerator)
        then:
        assert generator :"Generator cant be null"

    }

    def "MysqlGenerator should have the correct connection string"(){
        when:
        IGenerator generator = injector.getInstance(MysqlGenerator)
        then :
        assert generator.getConnectionString() == "jdbc:mysql://localhost:3306/INFORMATION_SCHEMA"
    }

    def "MysqlDBReflector should be properly injected"(){
        when:
        IGenerator generator = injector.getInstance(MysqlGenerator)
        then:
        assert generator.mysqlDbReflector : "Reflector cant be null"
    }

    def "Exclusion groups should be built properly" (){
        setup:
        injector = Guice.createInjector(new GeneratorInjector())
        when :
        IGenerator reflector = injector.getInstance(MysqlGenerator)
        reflector.buildExclusionGroups()
        List<String>actual = reflector.getExclusionGroupsList()
        List<String> expected = Arrays.asList("_aud","_audit","_history","_review")
        then :
        assert actual == expected : "Exclusion lists are not built properly"
    }

    def "getMasterListOfTables should return some data"(){
        setup :
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when :
        List<String> tables = generator.getMasterListOfTables()
        then :
        assert tables : "could not get the master list of tables"
        assert !tables.isEmpty() : "there are no tables in the aforementioned schema"
    }

    def "extractExistingHistoryTables should give us the existing history tables"(){
        given:
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        List<String> masterList = Arrays.asList("A","B","C","D","E","C_aud","D_audit","E_history")
        List<String> exclusionGroups = Arrays.asList("_aud","_history","_audit")
        List<String> expectedResult = Arrays.asList("C_aud","D_audit","E_history")
        List<String> actual = generator.extractExistingHistoryTables(masterList,exclusionGroups)

        expect:
        that actual, containsInAnyOrder(expectedResult.toArray())

    }

    def "extractTablesWithExistingHistoryTables should give us the tables for which the history tables exist"(){
        given:
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        List<String> masterList = Arrays.asList("ACCOUNT","ACCOUNT_TOKEN","C","D","E","C_aud","D_audit","E_history","heartbeat")
        List<String> exclusionGroups = Arrays.asList("_aud","_history","_audit")
        List<String> expectedResult = Arrays.asList("C","D","E")
        List<String> actual = generator.extractTablesWithExistingHistoryTables(masterList,exclusionGroups)

        expect:
        that actual, containsInAnyOrder(expectedResult.toArray())

    }


    def "getListOfTablesToReflectFor should give us the exact list fo tables to reflect for"(){
        setup:
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<String> actual = generator.getListOfTablesToReflectFor()

       then:
       assert   actual : "Cant be null"
       assert  !actual.isEmpty() : "Cant be empty"
    }

    def "getColumnData should return appropriate column values"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<String> actual = generator.getColumnDataFor("MAGNUS_PROPERTIES")
        then:
        assert  actual : "Cant be null"
        assert  actual.size() == 6 : "Cant be empty"
    }

    def "formulateTableData should return a proper list of tables"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<Table> actual = generator.formulateTableData()
        then:
        assert  actual : "Cant be null"
        assert  actual.size() != 0 : "Cant be empty"
    }

    def "prepareOutputDirectories ShouldPrepare the outputs in a correct way"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        OutputDirectory directory = generator.prepareOutputsDirectory()
        then:
        assert  directory.getLocation().exists() : "location not created"
        directory.getOutputMap().each{k,v ->
            assert v.getChangeLogFileName().exists() : "Changelog file not created"
        }
    }

    def "writeoutEncapsulatedChangeLogFile should create the wrapper file in a correct manner="(){

        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        OutputDirectory directory = generator.prepareOutputsDirectory()
        generator.writeoutEncapsulatedChangeLogFile(directory)
        then:
        assert  directory.getParentChangelogFile().size() !=0:  "fragment file cant be empty"

    }
  
    def "collectChangeSetMappings should return a proper mapping"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<Table> tables = generator.formulateTableData()
        ChangeSetMapping mapping =generator.collectChangeSetMappings(tables)
        then :
        assert  mapping : "Changeset not collected properly"
        assert  mapping.mappingOfTablesToChangeset.size() != 0: "No mappings collected"
    }


    def "writeoutChangeLogs should work properly"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<Table> tables = generator.formulateTableData()
        ChangeSetMapping mapping = generator.collectChangeSetMappings(tables)
        OutputDirectory dir = generator.prepareOutputsDirectory()
        generator.writeoutChangesetsTofile(dir,mapping)
        then :
        assert  mapping : "Changeset not collected properly"
        assert  mapping.mappingOfTablesToChangeset.size() != 0: "No mappings collected"

    }

    def "generateHistoryTables should work properly"(){
        injector = Guice.createInjector(new GeneratorInjector())
        IGenerator generator = injector.getInstance(MysqlGenerator)
        when:
        List<Table> tables = generator.generateHistoryTables()
        then :
        assert true

    }



}
