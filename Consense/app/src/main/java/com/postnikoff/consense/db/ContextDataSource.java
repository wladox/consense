package com.postnikoff.consense.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.postnikoff.consense.model.ContextParam;
import com.postnikoff.consense.model.ContextState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextDataSource {

    private static final String LOGTAG = "CONSENSE";

    private SQLiteOpenHelper dbhelper;
    private SQLiteDatabase database;

    private String[] allColumns = {
            ContextDBOpenHelper.COLUMN_ID,
            ContextDBOpenHelper.COLUMN_TYPE,
            ContextDBOpenHelper.COLUMN_CREATED,
            ContextDBOpenHelper.COLUMN_PARAMS
    };
    public ContextDataSource(Context context) {
        dbhelper = new ContextDBOpenHelper(context);
    }

    public void open() {
        database = dbhelper.getWritableDatabase();
        Log.i(LOGTAG, "Database opened");
    }

    public void close() {
        dbhelper.close();
        Log.i(LOGTAG, "Database closed");
    }

    public void cleanContextTable() {
        int rows = database.delete(ContextDBOpenHelper.TABLE_CONTEXT_STATE, "1", null);
        Log.i(LOGTAG, rows +" have been deleted from Context state table");
    }

    public ContextState create(ContextState state) {

        ContentValues values = new ContentValues(); // Map (key -> value) --> (column -> value)
        values.put(ContextDBOpenHelper.COLUMN_TYPE, state.getType());
        values.put(ContextDBOpenHelper.COLUMN_CREATED, state.getTimestamp());

        JSONArray jsonArray = new JSONArray();
        List<ContextParam> params = state.getParams();
        for (ContextParam cp : params) {
            jsonArray.put(cp.toJSON());
        }

        values.put(ContextDBOpenHelper.COLUMN_PARAMS, jsonArray.toString());

        long insertId = database.insert(ContextDBOpenHelper.TABLE_CONTEXT_STATE, null, values);

        state.setId(insertId);
        return state;
    }

    public List<ContextState> findAll() {
        List<ContextState> contextItems = new ArrayList<>();

        // query the database
        Cursor cursor = database.query(ContextDBOpenHelper.TABLE_CONTEXT_STATE, allColumns,
                null, null, null, null, null);

        Log.i(LOGTAG, "Returned + " + cursor.getCount() + " rows");

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                ContextState contextState = new ContextState();
                contextState.setId(cursor.getInt(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_ID)));
                contextState.setTimestamp(cursor.getLong(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_CREATED)));
                contextState.setType(cursor.getString(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_TYPE)));

                List<ContextParam> params = new ArrayList<>();
                try {
                    JSONArray paramJsonArray = new JSONArray(cursor.getString(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_PARAMS)));
                    for(int i = 0; i < paramJsonArray.length(); i++) {
                        JSONObject jsonObject = paramJsonArray.getJSONObject(i);
                        ContextParam param = new ContextParam();
                        param.setName(jsonObject.getString("name"));
                        param.setValue(jsonObject.getString("value"));
                        params.add(param);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                contextState.setParams(params);
                contextItems.add(contextState);
            }
        }
        return contextItems;
    }

    public JSONArray findAllAsJSON() {

        JSONArray array = new JSONArray();
        Cursor cursor = database.query(ContextDBOpenHelper.TABLE_CONTEXT_STATE, allColumns,
                null, null, null, null, null);
        if (cursor.getCount() > 0) {

            while (cursor.moveToNext()) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("id", cursor.getInt(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_ID)));
                    object.put("created", cursor.getLong(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_CREATED)));
                    object.put("type", cursor.getString(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_TYPE)));
                    object.put("params", new JSONArray(cursor.getString(cursor.getColumnIndex(ContextDBOpenHelper.COLUMN_PARAMS))));

                    array.put(object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return array;
    }
}
