package com.postnikoff.consense.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextParam {

    private String name;
    private String value;

    public ContextParam() {

    }

    public ContextParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JSONObject toJSON() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", getName());
            jsonObject.put("value", getValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;

    }
}
