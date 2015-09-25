package com.postnikoff.consense.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by CodeX on 26.08.2015.
 */
public class ContextDBOpenHelper extends SQLiteOpenHelper{

    private static final String LOGTAG = "CONSENSE";

    private static final String DATABASE_NAME       = "context.db";
    private static final int    DATABASE_VERSION    = 1;

    public static final String TABLE_CONTEXT_STATE = "context_state";
    public static final String COLUMN_ID           = "context_state_id";
    public static final String COLUMN_TYPE         = "type";
    public static final String COLUMN_CREATED      = "created";
    public static final String COLUMN_PARAMS       = "params";

    private static final String TABLE_CREATE        =
            "CREATE TABLE " + TABLE_CONTEXT_STATE + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TYPE + " TEXT, " +
            COLUMN_CREATED + " DATE, " +
            COLUMN_PARAMS + " TEXT" +
            ")";

    public ContextDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.i(LOGTAG, "Table has been created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTEXT_STATE);
        onCreate(db);
    }
}
