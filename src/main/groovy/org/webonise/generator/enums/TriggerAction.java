package org.webonise.generator.enums;

/**
 * Created by Webonise on 08/09/15.
 */
public enum TriggerAction {
    INSERT("INSERT"),
    UPDATE("UPDATE"),
    DELETE("DELETE");

    private String action;

    TriggerAction(String action) {
        this.action = action;
    }




}
