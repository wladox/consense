package com.postnikoff.consense.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.intel.context.item.AppsInstalled;
import com.intel.context.item.Item;
import com.postnikoff.consense.model.ContextParam;
import com.postnikoff.consense.model.ContextState;

import org.json.JSONArray;

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
      ContextDBOpenHelper.COLUMN_ID, ContextDBOpenHelper.COLUMN_TYPE, ContextDBOpenHelper.COLUMN_CREATED, ContextDBOpenHelper.COLUMN_PARAMS
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

    public ContextState create(ContextState state) {

        ContentValues values = new ContentValues(); // Map (key -> value) --> (column -> value)
        values.put(ContextDBOpenHelper.COLUMN_TYPE, state.getType());
        values.put(ContextDBOpenHelper.COLUMN_CREATED, state.getTimestamp());

        JSONArray jsonArray = new JSONArray();
        List<ContextParam> params = state.getParams();
        for (ContextParam cp : params) {
            jsonArray.put(cp.toString());
        }

        values.put(ContextDBOpenHelper.COLUMN_PARAMS, jsonArray.toString());

        long insertId = database.insert(ContextDBOpenHelper.TABLE_CONTEXT_STATE, null, values);

        state.setId(insertId);
        return state;
    }

    public List<Item> findAll() {
        List<Item> contextItems = new ArrayList<>();

        // query the database
        Cursor cursor = database.query(ContextDBOpenHelper.TABLE_CONTEXT_STATE, allColumns,
                null, null, null, null, null);

        Log.i(LOGTAG, "Returned + " + cursor.getCount() + " rows");

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {

            }
        }


        return contextItems;
    }
}
