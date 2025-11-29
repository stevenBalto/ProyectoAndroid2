package com.example.proyecto2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Ayudante para la creación y gestión de la base de datos SQLite de la aplicación.
 * Se encarga de crear la base de datos si no existe y de actualizarla si la versión cambia.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Nombre del archivo de la base de datos.
     */
    public static final String DB_NAME = "verduleria.db";

    /**
     * Versión de la base de datos. Si se cambia el esquema, se debe incrementar este número.
     */
    public static final int DB_VERSION = 4;

    /**
     * Constructor de la clase.
     * @param context El contexto de la aplicación.
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Se llama cuando la base de datos es creada por primera vez.
     * Aquí se define la estructura inicial de las tablas.
     * @param db La base de datos.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Crea la tabla "cliente" con las columnas necesarias
        db.execSQL("CREATE TABLE cliente (" +
                "cedula TEXT PRIMARY KEY," +   // Cédula del cliente (llave primaria)
                "nombre TEXT NOT NULL," +      // Nombre del cliente (no puede ser nulo)
                "telefono TEXT," +             // Teléfono del cliente
                "imagen BLOB," +               // Fotografía del cliente (almacenada como bytes)
                "audio BLOB," +                // Nota de audio (almacenada como bytes)
                "latitud TEXT," +              // Latitud de la ubicación
                "longitud TEXT" +              // Longitud de la ubicación
                ")");
    }

    /**
     * Se llama cuando la base de datos necesita ser actualizada.
     * La estrategia aquí es simple: elimina la tabla existente y la vuelve a crear.
     * ¡Esto borrará todos los datos existentes!
     * @param db La base de datos.
     * @param oldVersion La versión antigua de la base de datos.
     * @param newVersion La versión nueva de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Elimina la tabla "cliente" si ya existe
        db.execSQL("DROP TABLE IF EXISTS cliente");
        // Vuelve a crear la tabla con el nuevo esquema
        onCreate(db);
    }
}
