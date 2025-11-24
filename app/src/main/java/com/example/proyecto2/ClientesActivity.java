package com.example.proyecto2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class ClientesActivity extends AppCompatActivity {

    EditText txtCedula, txtNombre, txtTelefono;
    ImageView imgCliente;
    Button btnGuardar, btnCancelar;
    
    DatabaseHelper db;
    String cedulaEditar = null;
    Bitmap imagenBitmap;
    ActivityResultLauncher<Intent> lanzadorTomarFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        db = new DatabaseHelper(this);

        txtCedula = findViewById(R.id.txtCedula);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        imgCliente = findViewById(R.id.imgCliente);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        lanzadorTomarFoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                        Bundle extras = resultado.getData().getExtras();
                        if (extras != null) {
                            imagenBitmap = (Bitmap) extras.get("data");
                            imgCliente.setImageBitmap(imagenBitmap);
                        }
                    }
                }
        );

        // Recuperar datos si es edición
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("cedula")) {
            cedulaEditar = extras.getString("cedula");
            cargarDatos(cedulaEditar);
            txtCedula.setEnabled(false); // No editar la PK
        }

        btnGuardar.setOnClickListener(v -> guardarCliente());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void cargarDatos(String cedula) {
        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor c = readable.rawQuery("SELECT nombre, telefono, Imagen FROM cliente WHERE cedula = ?", new String[]{cedula});
        if (c.moveToFirst()) {
            txtCedula.setText(cedula);
            txtNombre.setText(c.getString(0));
            txtTelefono.setText(c.getString(1));
            
            byte[] blob = c.getBlob(2);
            if (blob != null) {
                imagenBitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                imgCliente.setImageBitmap(imagenBitmap);
            }
        }
        c.close();
    }

    private void guardarCliente() {
        String ced = txtCedula.getText().toString().trim();
        String nom = txtNombre.getText().toString().trim();
        String tel = txtTelefono.getText().toString().trim();

        if (ced.isEmpty() || nom.isEmpty()) {
            Toast.makeText(this, "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase writable = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nom);
        values.put("telefono", tel);

        if (imagenBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imagenBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put("Imagen", stream.toByteArray());
        }

        if (cedulaEditar != null) {
            writable.update("cliente", values, "cedula=?", new String[]{cedulaEditar});
            Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show();
        } else {
            values.put("cedula", ced);
            long res = writable.insert("cliente", null, values);
            if (res == -1) {
                Toast.makeText(this, "Error: Cédula ya existe", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Cliente guardado", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    // Método para el onClick del XML
    public void tomarFoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            lanzadorTomarFoto.launch(intent);
        } else {
             try {
                lanzadorTomarFoto.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No se puede abrir la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }
}