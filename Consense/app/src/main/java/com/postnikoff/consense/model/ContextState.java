package com.postnikoff.consense.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextState {

    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_APPS = "apps";
    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_PEDOMETER = "pedometer";

    private long id;
    private long timestamp;
    private String type;
    private List<ContextParam> params;

    public ContextState() {

    }

    public ContextState(long timestamp, String type) {
        this.timestamp = timestamp;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ContextParam> getParams() {
        return params;
    }

    public void setParams(List<ContextParam> params) {
        this.params = params;
    }

    public void addParam(ContextParam p) {
        this.params.add(p);
    }

}
