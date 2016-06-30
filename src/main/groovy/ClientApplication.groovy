import com.google.inject.Guice
import com.google.inject.Injector
import groovy.util.logging.Slf4j
import org.webonise.generator.core.interfaces.IGenerator
import org.webonise.generator.injector.GeneratorInjector

/**
 * Created by Webonise on 03/09/15.
 */
@Slf4j
class ClientApplication {
    public static void main(String [] args){
        Injector injector = Guice.createInjector(new GeneratorInjector())
        IGenerator mysqlGenerator = injector.getInstance(IGenerator.class)
        mysqlGenerator.generateHistoryTables();
        log.info("Done with generating the tables")
    }
}
