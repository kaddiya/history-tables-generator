package org.webonise.generator.injector

import com.google.inject.AbstractModule
import org.webonise.generator.core.impl.MysqlGenerator
import org.webonise.generator.core.interfaces.IGenerator
import org.webonise.generator.dbreflection.impl.MysqlDBReflector
import org.webonise.generator.dbreflection.interfaces.DBReflector
import org.webonise.generator.writers.LiquibaseChangelogWriter

/**
 * Created by Webonise on 03/09/15.
 */
class GeneratorInjector  extends  AbstractModule{
    @Override
    protected void configure() {
        bind(IGenerator).to(MysqlGenerator)
        bind(DBReflector).to(MysqlDBReflector)
        bind(LiquibaseChangelogWriter)
    }
}
