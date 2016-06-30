package org.webonise.generator.core.interfaces;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by Webonise on 03/09/15.
 */
public interface IGenerator {

    public void generateHistoryTables();

    public void buildExclusionGroups();

    List<String> getListOfTablesToReflectFor();


}
