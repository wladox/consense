package com.postnikoff.consense.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextState {

    private long id;
    private long timestamp;
    private String type;
    private List<ContextParam> params;

    public ContextState() {

    }

    public enum ContextType {
        LOCATION, APPS, ACTIVITY, MUSIC, AUDIO, PEDOMETER
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
