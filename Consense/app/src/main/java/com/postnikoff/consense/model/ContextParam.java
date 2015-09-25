package com.postnikoff.consense.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextParam {

    private String name;
    private String type;
    private String value;

    public ContextParam() {

    }

    public ContextParam(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {

        JSONObject param = new JSONObject();
        try {
            param.put("name", getName());
            param.put("type", getType());
            param.put("value", getValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return param.toString();

    }
}
