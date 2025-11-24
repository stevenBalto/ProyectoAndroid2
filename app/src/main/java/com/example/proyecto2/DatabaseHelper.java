package com.example.proyecto2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "verduleria.db";
    public static final int DB_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cliente (" +
                "cedula TEXT PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "telefono TEXT," +
                "Imagen BLOB)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cliente");
        onCreate(db);
    }
}